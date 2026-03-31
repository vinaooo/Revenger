import sys, os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from utils import load_env
import requests
from fetch_icon import search_sgdb_by_text

load_env()
SGDB_API_KEY = os.environ.get("SGDB_API_KEY")

gid = search_sgdb_by_text("metroid zero mission")
print(f"Game ID: {gid}")
url = f"https://www.steamgriddb.com/api/v2/icons/game/{gid}?mimes=image/png"
headers = {"Authorization": f"Bearer {SGDB_API_KEY}"}
resp = requests.get(url, headers=headers).json()
if resp.get('data'):
    print(resp['data'][0])
else:
    print("No data")
