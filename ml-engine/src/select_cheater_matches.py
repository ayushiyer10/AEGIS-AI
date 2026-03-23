import pandas as pd

df = pd.read_csv("data/cs2cd_sample.csv")

print("Columns in dataset:")
print(df.columns.tolist())
