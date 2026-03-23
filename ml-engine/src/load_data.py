import pandas as pd
from utils.path import resource_path

# Accepted velocity column patterns (lowercase)
VELOCITY_SETS = [
    ("velocity_x", "velocity_y", "velocity_z"),
    ("vel_x", "vel_y", "vel_z"),
]

ANGLE_COLUMNS = ["pitch", "yaw"]

def load_ticks():
    csv_path = resource_path("data/cs2cd_sample.csv")
    df = pd.read_csv(csv_path)

    df.columns = [c.strip().lower() for c in df.columns]

    # Detect velocity columns dynamically
    velocity_cols = None
    for candidate in VELOCITY_SETS:
        if all(col in df.columns for col in candidate):
            velocity_cols = list(candidate)
            break

    if velocity_cols is None:
        raise ValueError(
            f"No valid velocity columns found. Available columns:\n{df.columns.tolist()}"
        )

    # Validate angle columns
    missing_angles = [c for c in ANGLE_COLUMNS if c not in df.columns]
    if missing_angles:
        raise ValueError(f"Missing angle columns: {missing_angles}")

    keep_cols = ["steamid"] + velocity_cols + ANGLE_COLUMNS
    df = df[keep_cols].dropna()

    print("Using velocity columns:", velocity_cols)
    print("Loaded rows:", len(df))

    return df
