import json
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ContractSchemas(unittest.TestCase):
    def node(self, program):
        result = subprocess.run(['node', '--input-type=module', '-e', program], cwd=ROOT, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stderr.decode())

    def test_complete_sdl_and_proto_match_source_groups_once(self):
        source = ROOT.joinpath('docs/04-spoolin-techdoc.md').read_text()
        for section, language, path, count in [
            ('5.1', 'graphql', 'contracts/graphql/schema.graphql', 6),
            ('5.2', 'protobuf', 'contracts/grpc/src/main/proto/spoolin/v1/spoolin.proto', 10),
        ]:
            groups = []
            for match in re.finditer(r'^#### (' + re.escape(section) + r'\.\d+) .*?\n(.*?)(?=^#{3,4} |\Z)', source, re.M | re.S):
                if match.group(1) == '5.1.7':
                    continue
                groups.extend(re.findall(r'```' + language + r'\n(.*?)\n```', match.group(2), re.S))
            self.assertEqual(len(groups), count)
            self.assertEqual(ROOT.joinpath(path).read_text(), '\n\n'.join(groups) + '\n')

    def test_browser_operation_inventory_and_read_arguments(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {buildSchema, parse, validate} from 'graphql';
const schema=buildSchema(readFileSync('contracts/graphql/schema.graphql','utf8'));
assert.deepEqual(Object.keys(schema.getQueryType().getFields()), ['session','policy','titles','title','people','person','titleCredits','personCredits','genres','creditRoles','managedTitle','managedPerson','managedCredits','managedTitles','managedPeople','deletionImpact','operation','drafts','audit']);
assert.deepEqual(Object.keys(schema.getMutationType().getFields()), ['register','signIn','signOut','createMovie','updateMovie','movieLifecycle','createPerson','updatePerson','personLifecycle','createRole','addCredit','updateCredit','removeCredit','uploadArtwork','cancelArtwork','saveDraft','discardDraft']);
assert.ok(validate(schema,parse('query { titles(filter:{kind:WRONG}) { nodes { id } } }')).length);
assert.ok(validate(schema,parse('query { title(id:"190db10d-02b8-437e-8e7c-ece9c105fe2a", expectedVersion:"1") { id } }')).length);
for(const field of Object.values(schema.getQueryType().getFields())) assert.ok(!field.args.some(a=>['expectedVersion','policyRevision','operationId'].includes(a.name)));
assert.equal(schema.getType('AuditEvent').getFields().outcome.type.toString(),'AuditOutcome!');
assert.equal(schema.getType('AuditEvent').getFields().subjectKind.type.toString(),'String!');
assert.equal(schema.getMutationType().getFields().saveDraft.args.find(a=>a.name==='id').type.toString(),'UUID!');
assert.equal(schema.getMutationType().getFields().discardDraft.type.toString(),'DraftDiscardResult!');
""")

    def test_output_registry_fragments_and_local_reference_closure(self):
        source = ROOT.joinpath('docs/04-spoolin-techdoc.md').read_text()
        start = source.index('### 5.10 MCP output schema registry')
        end = source.index('### 5.11 JSON projections', start)
        merged = {}
        for body in re.findall(r'```json\n(.*?)\n```', source[start:end], re.S):
            for key, value in json.loads(body).items():
                if isinstance(value, dict):
                    self.assertFalse(set(merged.get(key, {})) & set(value))
                    merged.setdefault(key, {}).update(value)
                else:
                    merged[key] = value
        self.assertEqual(json.loads(ROOT.joinpath('contracts/json/mcp-outputs.schema.json').read_text()), merged)
        self.node(r"""
import {readFileSync} from 'node:fs';
import {checkReferences} from './build-tools/contracts/validation.mjs';
for (const name of ['mcp-inputs','mcp-outputs','draft','policy']) checkReferences(JSON.parse(readFileSync(`contracts/json/${name}.schema.json`,'utf8')));
""")

    def test_recipient_projection_rejects_private_and_unknown_fields(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {validateOutput} from './build-tools/contracts/validation.mjs';
const receipt={operationId:'190db10d-02b8-437e-8e7c-ece9c105fe2a',state:'SUCCEEDED',resultExpired:false,version:'9007199254740993'};
assert.equal(validateOutput('movie_operation',receipt),receipt);
for(const name of ['approve_movie','reject_movie']) assert.deepEqual(validateOutput(name,{...receipt,problem:null,resultId:null}),{...receipt,problem:null,resultId:null});
for(const key of ['password','challenge','ui_challenge','accountEmail','internalState']) assert.throws(()=>validateOutput('movie_operation',{...receipt,[key]:'unexposed'}),/Undeclared/);
assert.throws(()=>validateOutput('movie_operation',{...receipt,version:'9223372036854775808'}),/range/);
assert.throws(()=>validateOutput('movie_operation',{...receipt,operationId:receipt.operationId.toUpperCase()}),/UUID/);
assert.throws(()=>validateOutput('movie_operation',{...receipt,state:'WRONG'}),/enum/);
assert.throws(()=>validateOutput('movie_operation',{...receipt,problem:{code:'SAFE',message:'Safe message',fields:[],retryAfterMs:null,sql:'private'}}),/schema/);
assert.throws(()=>validateOutput('movie_context',{titles:[],genres:[],roles:[],policyRevision:'0'.repeat(64),privateRow:{}}),/Undeclared/);
""")

    def test_input_maps_bind_both_trusted_actions_and_reject_forged_fields(self):
        source = ROOT.joinpath('docs/04-spoolin-techdoc.md').read_text()
        start = source.index('### 9.1 MCP tool input schemas')
        end = source.index('### 9.1 Secure MCP Tunnel', start)
        fragments = [json.loads(body) for body in re.findall(r'```json\n(.*?)\n```', source[start:end], re.S)]
        expected = {key: value for fragment in fragments[:3] for key, value in fragment.items()}
        expected.update({'approve_movie': fragments[3], 'reject_movie': fragments[3]})
        self.assertEqual(json.loads(ROOT.joinpath('contracts/json/mcp-inputs.schema.json').read_text()), expected)
        self.node(r"""
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateInput} from './build-tools/contracts/validation.mjs';
const registry=JSON.parse(readFileSync('contracts/json/mcp-inputs.schema.json','utf8'));
assert.deepEqual(Object.keys(registry),['movie_context','propose_movie','movie_operation','approve_movie','reject_movie']);
assert.deepEqual(registry.approve_movie,registry.reject_movie);
const input={proposalId:'190db10d-02b8-437e-8e7c-ece9c105fe2a',revision:1,contentHash:'0'.repeat(64),challenge:Buffer.alloc(32).toString('base64url')};
for(const tool of ['approve_movie','reject_movie']) {assert.equal(validateInput(tool,input),input);assert.throws(()=>validateInput(tool,{...input,approved:true}),/Undeclared/);assert.throws(()=>validateInput(tool,{...input,challenge:input.challenge+'='}),RangeError);}
assert.throws(()=>validateInput('movie_context',{query:'safe',limit:21}),/bounds/);
assert.throws(()=>validateInput('movie_context',{query:'safe',accountId:input.proposalId}),/Undeclared/);
const proposal={operationId:input.proposalId,title:'Arrival',synopsis:'A visitor arrives.',genreIds:[input.proposalId],release:{year:2016}};
assert.equal(validateInput('propose_movie',proposal),proposal);
assert.throws(()=>validateInput('propose_movie',{...proposal,proposalId:input.proposalId}),/together/);
assert.throws(()=>validateInput('propose_movie',{...proposal,sources:[{url:'file:///private/value',field:'title',reuseBasis:'Claim'}]}),/evidence/);
""")

    def test_policy_json_shapes_validate_formats_and_private_additions(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateSchema} from './build-tools/contracts/validation.mjs';
const schema=JSON.parse(readFileSync('contracts/json/policy.schema.json','utf8'));
const keys=['registrationName','personName','title','originalTitle','synopsis','creditRole','characterName','biography','email','password','search','sourceUrl','reuseBasis'];
const policy={textBounds:Object.fromEntries(keys.map(key=>[key,{minCodePoints:0,maxCodePoints:100,required:false,normalization:key==='password'?'EXACT':'TRIM_WHITE_SPACE'}])),artwork:{maxInputBytes:'5242880',formats:['JPEG','PNG','WEBP'],maxDimensionPx:4096,maxPixels:'40000000',maxDecodedBytes:'160000000',maxOutputBytes:'10485760'},requestLimits:{metadataBytes:131072,uploadEnvelopeBytes:7100000,graphqlQueryBytes:16384,maxDepth:8,maxFields:200,maxAliases:20,maxCost:1000,defaultPage:24,maxPage:100}};
assert.equal(validateSchema(schema,policy),policy);
assert.throws(()=>validateSchema(schema,{...policy,email:'private'}),/Undeclared/);
assert.throws(()=>validateSchema(schema,{...policy,artwork:{...policy.artwork,maxPixels:'9223372036854775808'}}),/range/);
assert.throws(()=>validateSchema(schema,{...policy,artwork:{...policy.artwork,formats:['SVG']}}),/enum/);
assert.throws(()=>validateSchema(schema,{...policy,textBounds:{...policy.textBounds,password:{...policy.textBounds.password,normalization:'TRIM_WHITE_SPACE'}}}),/constant/);
""")

    def test_drafts_preserve_incomplete_blank_and_outdated_values_exactly(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {validateDraft} from './build-tools/contracts/validation.mjs';
const movie={title:'  ',synopsis:'x'.repeat(5001),genreIds:[],release:{month:2},runtimeMinutes:-1,synopsisLanguage:'',countries:[]};
assert.equal(validateDraft('MOVIE',movie),movie);
assert.equal(validateDraft('MOVIE',movie).title,'  ');
assert.equal(validateDraft('PERSON',{}).name,undefined);
assert.deepEqual(validateDraft('CREDIT',{characters:['  John / Jane  ','']}),{characters:['  John / Jane  ','']});
for(const key of ['password','file','csrfToken','accountId','challenge','payloadVersion']) assert.throws(()=>validateDraft('MOVIE',{[key]:'forbidden'}),/Undeclared/);
assert.throws(()=>validateDraft('PERSON',{biography:'x'.repeat(65536)}),/Envelope/);
assert.throws(()=>validateDraft('CREDIT',{characters:[null]}),/type/);
assert.throws(()=>validateDraft('MOVIE',{title:'\ud800'}),/Unicode/);
assert.throws(()=>validateDraft('MOVIE',{release:{year:0}}),/bounds/);
""")

    def test_omission_clear_order_and_whole_date_replacement(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {patchValues,maskedValues} from './build-tools/contracts/validation.mjs';
const before={title:'Original',synopsis:'Synopsis',originalTitle:'Original title',countries:['ID'],release:{year:2001,month:6,day:12}};
assert.deepEqual(patchValues('MOVIE',before,{}),before);
assert.equal(patchValues('MOVIE',before,{originalTitle:null}).originalTitle,null);
assert.equal(patchValues('MOVIE',before,{originalTitle:'   '}).originalTitle,null);
assert.deepEqual(patchValues('MOVIE',before,{countries:[]}).countries,[]);
assert.deepEqual(patchValues('MOVIE',before,{release:{year:2024}}).release,{year:2024});
for(const patch of [{title:null},{title:' '},{synopsis:null},{release:null},{release:{month:2}},{genreIds:[]},{countries:null},{synopsisLanguage:null}]) assert.throws(()=>patchValues('MOVIE',before,patch));
assert.deepEqual(before.release,{year:2001,month:6,day:12});
assert.equal(before.originalTitle,'Original title');
assert.deepEqual(patchValues('CREDIT',{}, {characters:['  John / Jane  ','','Doctor / Patient','  ']}).characters,['John / Jane','Doctor / Patient']);
assert.deepEqual(patchValues('CREDIT',{}, {characters:['  ','']}).characters,[]);
assert.throws(()=>patchValues('CREDIT',{}, {characters:['John',' John ']}),/Duplicate/);
assert.throws(()=>patchValues('CREDIT',{}, {characters:null}),/REQUIRED/);
assert.throws(()=>patchValues('CREDIT',{}, {personId:'190db10d-02b8-437e-8e7c-ece9c105fe2a'}),/Undeclared/);
assert.throws(()=>patchValues('CREDIT',{}, {titleId:'190db10d-02b8-437e-8e7c-ece9c105fe2a'}),/Undeclared/);
assert.deepEqual(maskedValues('MOVIE',before,{originalTitle:'ignored'},[]),before);
assert.equal(maskedValues('MOVIE',before,{},['original_title']).originalTitle,null);
assert.deepEqual(maskedValues('MOVIE',before,{},['countries']).countries,[]);
assert.throws(()=>maskedValues('MOVIE',before,{},['genre_ids']),/REQUIRED/);
assert.throws(()=>maskedValues('MOVIE',before,{},['release.year']),/mask/);
""")

    def test_draft_null_year_is_preserved_but_final_year_is_required(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {validateDraft,validateInput,patchValues} from './build-tools/contracts/validation.mjs';
const draft={release:{year:null}};
assert.equal(validateDraft('MOVIE',draft),draft);
assert.deepEqual(draft,{release:{year:null}});
for (const release of [{},{year:null},{month:2}]) {
  assert.throws(()=>patchValues('MOVIE',{}, {release}),/REQUIRED/);
  assert.throws(()=>validateInput('propose_movie',{operationId:'190db10d-02b8-437e-8e7c-ece9c105fe2a',title:'Movie',synopsis:'Synopsis',genreIds:['190db10d-02b8-437e-8e7c-ece9c105fe2a'],release}));
}
for (const year of [0,10000,1.5]) assert.throws(()=>validateDraft('MOVIE',{release:{year}}));
""")

    def test_ordinary_patch_unicode_whitespace_and_nul_differ_from_drafts(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {patchValues,validateDraft} from './build-tools/contracts/validation.mjs';
assert.equal(patchValues('MOVIE',{}, {originalTitle:'\u0085'}).originalTitle,null);
assert.throws(()=>patchValues('MOVIE',{}, {title:'\u0085'}),/REQUIRED/);
for (const key of ['title','originalTitle']) {
  assert.equal(patchValues('MOVIE',{}, {[key]:'\uFEFF'})[key],'\uFEFF');
  assert.equal(patchValues('MOVIE',{}, {[key]:'\u0085A  e\u0301\u0085'})[key],'A  e\u0301');
  assert.throws(()=>patchValues('MOVIE',{}, {[key]:'A\u0000B'}),/NUL/);
}
assert.deepEqual(patchValues('CREDIT',{}, {characters:['\u0085','\uFEFF','\u0085John / Jane\u0085']}).characters,['\uFEFF','John / Jane']);
assert.throws(()=>patchValues('CREDIT',{}, {characters:['A\u0000B']}),/NUL/);
assert.throws(()=>patchValues('MOVIE',{}, {countries:['I\u0000D']}),/NUL/);
const draft={title:'\u0085\uFEFFA  e\u0301\u0085',originalTitle:'\u0085'};
assert.equal(validateDraft('MOVIE',draft),draft);
assert.equal(draft.title,'\u0085\uFEFFA  e\u0301\u0085');
assert.equal(draft.originalTitle,'\u0085');
assert.deepEqual(validateDraft('CREDIT',{characters:['\u0085','\uFEFF']}),{characters:['\u0085','\uFEFF']});
""")
