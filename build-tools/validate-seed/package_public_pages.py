#!/usr/bin/env python3
"""Explicitly package reviewed public HTML evidence, posters and original prose.

Run only after inspecting all 200 title/poster matches and supplied synopsis prose.
The previous manifest is retained once for safe upgrades. Startup never invokes this.
"""
import argparse,sys,html,json,re,shutil,uuid
from pathlib import Path
from validate_manifest import checksum
parser=argparse.ArgumentParser(description='Package reviewed public-page posters and original synopses.')
parser.add_argument('--cache',type=Path,required=True)
parser.add_argument('--synopses',type=Path,required=True,help='Reviewed JSON mapping title UUID to original English synopsis')
args=parser.parse_args()
root=Path(__file__).resolve().parents[2]/'data/seed';cache=args.cache;m=json.load(open(root/'manifest.json'));syn=json.load(open(args.synopses))
assert len(syn)==200
base=root/'baselines/wikidata-2026-10-v1.json';base.parent.mkdir(exist_ok=True)
if not base.exists():shutil.copyfile(root/'manifest.json',base)
(root/'posters').mkdir(exist_ok=True);ledger=[];files=[]
langs={'English':'en','German':'de','French':'fr','Portuguese':'pt','Japanese':'ja','Norwegian':'no','Italian':'it','Swedish':'sv','Spanish':'es','Finnish':'fi','Danish':'da','Korean':'ko','Dutch':'nl','Chinese':'zh','Mandarin':'zh','Hindi':'hi','Arabic':'ar','Russian':'ru'}
countries={'United States':'US','United Kingdom':'GB','Germany':'DE','France':'FR','Japan':'JP','Italy':'IT','Brazil':'BR','Norway':'NO','Sweden':'SE','Portugal':'PT','Spain':'ES','Austria':'AT','Canada':'CA','Hungary':'HU','Finland':'FI','Denmark':'DK','South Korea':'KR','Australia':'AU','China':'CN','India':'IN','New Zealand':'NZ','Netherlands':'NL','Belgium':'BE','Ireland':'IE'}
for r in m['records']:
 uid=r['stableUUID'];p=json.load(open(cache/(uid+'.json')));s=(cache/(uid+'.html')).read_text();n=r['normalized'];changed=['synopsis'];n['synopsis']=syn[uid]
 for label,field in [('Original Title','originalTitle'),('Original Name','originalTitle'),('Original Language','originalLanguage')]:
  match=re.search(r'(?:<bdi>)?'+label+r'(?:</bdi>)?</strong>\s*([^<]+)',s)
  if match:
   val=html.unescape(match[1].strip());val=langs.get(val) if field=='originalLanguage' else val
   if val:n[field]=val;changed.append(field)
 for raw in re.findall(r'<script[^>]*type="application/ld\+json"[^>]*>(.*?)</script>',s,re.S):
  try:j=json.loads(raw[raw.index('{'):raw.rindex('}')+1])
  except ValueError:continue
  cs=[countries[c['name']] for c in j.get('countryOfOrigin',[]) if c.get('name') in countries]
  if cs:n['countries']=list(dict.fromkeys(cs));changed.append('countries')
  if r['kind']=='MOVIE':
   mt=re.fullmatch(r'PT(?:(\d+)H)?(?:(\d+)M)?',j.get('duration',''))
   if mt and any(mt.groups()):n['runtimeMinutes']=60*int(mt[1] or 0)+int(mt[2] or 0);changed.append('runtimeMinutes')
 corrections={'Ginger and Fred':(1986,1,22,'https://www.bfi.org.uk/features/late-federico-fellini'), 'Kiss the Girls':(1997,9,29,'https://catalog.afi.com/Catalog/moviedetails/60870'), 'Dexter':(2006,10,1,'https://www.paramountplus.com/sneak-peak/dexter-20th-anniversary/'), 'Days of Our Lives':(1965,11,8,'https://www.televisionacademy.com/features/news/latest-news/days-our-lives-commemorates-45-year-milestone')}
 if n['title'] in corrections:
  year,month,day,corroboration=corrections[n['title']]
  n['release']={'year':year,'month':month,'day':day,'precision':'DAY'};changed.append('release')
  p['releaseCorroboration']=corroboration
 if n['title']=='Crash':p['releaseNote']='Retains 2004 festival premiere; source page title reflects 2005 theatrical release.'
 if n['title']=='Babylon 5':p['releaseNote']='Retains 1993 pilot premiere; source page title reflects 1994 regular series start.'
 for field in sorted(set(changed)):
  r['sources']=[x for x in r['sources'] if x['field']!=field]
  r['sources'].append({'field':field,'url':p['releaseCorroboration'] if field=='release' and r['kind']=='TV' else p['sourceUrl'],'retrievedAt':p['retrievedAt'],'license':'not-specified','permissionBasis':'Public webpage source recorded; no open-license claim.'})
 artwork={k:p[k] for k in ('sha256','width','height','sourceUrl','imageUrl','retrievedAt')}
 artwork.update(file='posters/'+uid+'.jpg',mimeType='image/jpeg',assetId=str(uuid.uuid5(uuid.UUID(m['namespaceUUID']),'SEED_ARTWORK:'+uid+':'+p['sha256'])),holdId=str(uuid.uuid5(uuid.UUID(m['namespaceUUID']),'SEED_ARTWORK_REFERENCE:'+uid+':'+p['sha256'])),sourceName='TMDB',creator=None,license=None)
 r['artwork']=artwork;files.append(dict(artwork,titleId=uid));shutil.copyfile(cache/(uid+'.jpg'),root/artwork['file']);r['contentSHA256']=checksum(r)
 p['artworkEdition']='Current series poster; may reflect later-season branding.' if r['kind']=='TV' else 'Title poster selected by the source page.'
 ledger.append(p)
m['distribution']={'years':dict(sorted(__import__('collections').Counter(str(r['normalized']['release']['year']) for r in m['records']).items())),'months':dict(sorted(__import__('collections').Counter(str(r['normalized']['release']['month'] or 'unknown') for r in m['records']).items()))}
m.update(manifestVersion='public-pages-2026-10-v2',sourcePolicyVersion='public-pages-v1',baselineManifest='baselines/wikidata-2026-10-v1.json',licenseSummary='Wikidata structured facts CC0-1.0; original factual synopses; TMDB public-page metadata and posters attributed without an open-license claim.',files=files)
m['snapshotSHA256']=checksum(m);(root/'manifest.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n');(root/'sources').mkdir(exist_ok=True);(root/'sources/tmdb-pages.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n')
print('Packaged',len(files),'posters')
