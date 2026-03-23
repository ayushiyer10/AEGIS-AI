from feature_extractor import extract_mouse_features

def build_match_features(df):
    all_features = []

    for _, row in df.iterrows():
        events = row.get("events", [])
        features = extract_mouse_features(events)
        all_features.append(features)

    return all_features
