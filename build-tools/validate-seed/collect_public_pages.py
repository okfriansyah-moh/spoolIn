#!/usr/bin/env python3
"""Explicit operator preparation from public HTML and linked JPEG files.

Requires Pillow for complete image decoding. Output remains in the chosen review
cache until an operator reviews provenance, identity, artwork and original synopses.
No API requests or automatic manifest publication are performed.
"""
import argparse,concurrent.futures,hashlib,html,json,re,time,urllib.request
from pathlib import Path
from PIL import Image
parser=argparse.ArgumentParser(description='Prepare posters from public HTML pages; never run at startup.')
parser.add_argument('--mappings',type=Path,required=True,help='Frozen local identity mapping snapshot')
parser.add_argument('--cache',type=Path,required=True)
args=parser.parse_args()
ROOT=Path(__file__).resolve().parents[2];CACHE=args.cache;CACHE.mkdir(parents=True,exist_ok=True)
entities=json.loads(args.mappings.read_text())
manifest=json.load(open(ROOT/'data/seed/manifest.json'))
def get(url):
 for attempt in range(3):
  try:
   request=urllib.request.Request(url,headers={'User-Agent':'SpoolIn seed preparation/1.0','Accept-Language':'en-US,en;q=0.9'})
   with urllib.request.urlopen(request,timeout=35) as response:
    if not response.url.startswith(('https://www.themoviedb.org/','https://media.themoviedb.org/','https://image.tmdb.org/')):raise ValueError('Unexpected redirect')
    data=response.read(8_000_001)
    if len(data)>8_000_000:raise ValueError('Oversized response')
    return data,response.url
  except Exception:
   if attempt==2:raise
   time.sleep(2+attempt)
def collect(row):
 uid=row['stableUUID'];out=CACHE/(uid+'.json')
 if out.exists():return json.load(open(out))
 prop='P4947' if row['kind']=='MOVIE' else 'P4983';ids=entities[row['externalId']]['claims'][prop];tmdb=next(c['mainsnak']['datavalue']['value'] for c in ids if c['rank']!='deprecated')
 url='https://www.themoviedb.org/'+('movie/' if row['kind']=='MOVIE' else 'tv/')+tmdb+'?language=en-US'
 raw,resolved=get(url);s=raw.decode();(CACHE/(uid+'.html')).write_bytes(raw)
 images=re.findall(r'<meta property="og:image" content="([^"]+)"',s)
 image=next(i for i in images if '/w500/' in i and i.endswith('.jpg'))
 data,_=get(html.unescape(image));path=CACHE/(uid+'.jpg');path.write_bytes(data)
 with Image.open(path) as im:
  im.load();width,height=im.size
  if im.format!='JPEG' or height<=width:raise ValueError('Not portrait JPEG')
 title=html.unescape(re.search(r'<meta property="og:title" content="([^"]+)"',s).group(1))
 result={'titleId':uid,'title':title,'sourceUrl':resolved,'imageUrl':image,'width':width,'height':height,'sha256':hashlib.sha256(data).hexdigest(),'retrievedAt':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),'htmlSHA256':hashlib.sha256(raw).hexdigest()}
 out.write_text(json.dumps(result,indent=2));time.sleep(.4);return result
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
 futures={pool.submit(collect,row):row for row in manifest['records']}
 for number,f in enumerate(concurrent.futures.as_completed(futures),1):
  row=futures[f]
  try:r=f.result();print(number,row['normalized']['title'],'OK',r['width'],r['height'],flush=True)
  except Exception as e:print(number,row['normalized']['title'],'FAIL',repr(e),flush=True)
