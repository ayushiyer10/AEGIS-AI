import pandas as pd

df = pd.read_csv("data/cs2cd_sample.csv")  # adjust path

print(df.head())
print("\nColumn types:\n", df.dtypes)
