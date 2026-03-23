from pathlib import Path

root = Path(".")

print("Searching for cheater_present folder...\n")

for p in root.rglob("cheater_present"):
    print("FOUND:", p)
