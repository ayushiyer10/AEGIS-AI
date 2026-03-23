from flask import Flask, request
import requests
import time

app = Flask(__name__)

ENGINE_URL = "http://localhost:8080/api/ingest"

@app.route("/ingest", methods=["POST"])
def ingest():

    data = request.json

    payload = {
        "mouse": data.get("mouse", []),
        "keyboard": data.get("keyboard", []),
        "velocity": data.get("velocity", 0),
        "isCheater": data.get("isCheater", False)
    }

    try:
        requests.post(ENGINE_URL, json=payload, timeout=1)
    except:
        pass

    return {"ok": True}

app.run(port=9000)
