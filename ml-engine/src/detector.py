import pandas as pd
from sklearn.ensemble import IsolationForest

def detect_anomalies(features: pd.DataFrame) -> pd.DataFrame:
    model = IsolationForest(
        n_estimators=100,
        contamination=0.1,
        random_state=42
    )

    numeric = features.drop(columns=["steamid"])
    anomalies = model.fit_predict(numeric)

    features["anomaly"] = anomalies
    features["is_cheater"] = features["anomaly"] == -1

    print("Anomalies detected:", features["is_cheater"].sum())
    return features
