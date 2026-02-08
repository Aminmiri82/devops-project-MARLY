import xml.etree.ElementTree as ET
import os

path = r"c:\Users\malad\Documents\M1 Miage APP\DevOps\devops-project-MARLY\Mavigo\build\reports\jacoco\test\jacocoTestReport.xml"
if not os.path.exists(path):
    print(f"Error: {path} not found")
    exit(1)

tree = ET.parse(path)
root = tree.getroot()

print("="*60)
print("SENIOR DEV COVERAGE ANALYSIS")
print("="*60)

for c in root.findall("counter"):
    if c.get("type") == "INSTRUCTION":
        missed = int(c.get("missed") or 0)
        covered = int(c.get("covered") or 0)
        total = missed + covered
        pct = covered/total*100
        print(f"OVERALL INSTRUCTION COVERAGE: {pct:.2f}% ({covered}/{total})")
        print(f"Target 85%: {int(total * 0.85)} covered (Need {max(0, int(total * 0.85) - covered)} more)")

print("\n" + "-"*60)
print(f"{'Class/Package':40} | {'Missed':>6} | {'Cov %':>5}")
print("-"*60)

gaps = []
for p in root.findall("package"):
    pkg_name = p.get("name").replace("/", ".")
    for cl in p.findall("class"):
        cl_name = cl.get("name").replace("/", ".")
        if "$" in cl_name: continue # Skip inner classes for brevity
        for c in cl.findall("counter"):
            if c.get("type") == "INSTRUCTION":
                miss = int(c.get("missed") or 0)
                cov = int(c.get("covered") or 0)
                total = miss + cov
                if total > 0:
                    pct = cov/total*100
                    if miss > 20: # Only show significant gaps
                        gaps.append((cl_name, miss, pct))

gaps.sort(key=lambda x: x[1], reverse=True)
for name, miss, pct in gaps[:15]:
    displayName = name.split(".")[-1]
    print(f"{displayName:40} | {miss:6} | {pct:4.1f}%")

print("="*60)
