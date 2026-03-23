import numpy as np

def extract_mouse_features(events):
    if not events:
        return {
            "avg_dx": 0,
            "avg_dy": 0,
            "std_dx": 0,
            "std_dy": 0,
            "event_count": 0
        }

    dx = [e.get("dx", 0) for e in events if "dx" in e]
    dy = [e.get("dy", 0) for e in events if "dy" in e]

    return {
        "avg_dx": np.mean(dx) if dx else 0,
        "avg_dy": np.mean(dy) if dy else 0,
        "std_dx": np.std(dx) if dx else 0,
        "std_dy": np.std(dy) if dy else 0,
        "event_count": len(events)
    }
