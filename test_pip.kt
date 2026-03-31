import os
import requests
from utils import load_env

load_env()
SGDB_API_KEY = os.environ.get("SGDB_API_KEY")
url = f"https://www.steamgriddb.com/api/v2/icons/game/12345?mimes=image/png"
headers = {"Authorization": f"Bearer {SGDB_API_KEY}"}
print(requests.get(url, headers=headers).json())
