import json
import os

from load_data import load_ticks
from feature_engineering import extract_player_features
from detector import detect_anomalies
from utils.path import resource_path

def main():
    df = load_ticks()
    features = extract_player_features(df)
    results = detect_anomalies(features)

    cheaters = results[results["is_cheater"] == True]

    output_dir = resource_path("output")
    os.makedirs(output_dir, exist_ok=True)

    output_file = os.path.join(output_dir, "cheaters.json")
    cheaters.to_json(output_file, orient="records", indent=2)

    print(f"Exported {len(cheaters)} cheaters")

if __name__ == "__main__":
    main()
