import requests

ENGINE_URL = "http://localhost:8080/api/ingest"

def send_to_engine(data):

    try:
        requests.post(ENGINE_URL, json=data, timeout=1)
    except Exception:
        pass
