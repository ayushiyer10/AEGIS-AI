import pandas as pd

def extract_player_features(df: pd.DataFrame) -> pd.DataFrame:
    features = df.groupby("steamid").agg(
        vel_x_std=("velocity_x", "std"),
        vel_y_std=("velocity_y", "std"),
        vel_z_std=("velocity_z", "std"),
        pitch_std=("pitch", "std"),
        yaw_std=("yaw", "std"),
        accuracy_mean=("velocity_x", "mean"),
    ).reset_index()

    features = features.dropna()
    print("Extracted player features:", features.shape)
    return features
