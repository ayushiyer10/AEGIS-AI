import math

def build_features(raw):
    dx, dy, clicks = map(int, raw["mouse"].split(","))

    speed = math.sqrt(dx*dx + dy*dy)
    accuracy = clicks / max(speed, 1)

    return {
        "pitch": dy,
        "yaw": dx,
        "velocity": speed,
        "accuracy": accuracy
    }
