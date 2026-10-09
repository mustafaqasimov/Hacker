#!/usr/bin/env python3
"""Enforce 80% line coverage across API/domain modules and the standalone migrator."""
from pathlib import Path
import xml.etree.ElementTree as ET
base=Path(__file__).resolve().parents[1]
reports=[base/'app/target/site/jacoco-aggregate/jacoco.xml',base/'migration/target/site/jacoco/jacoco.xml']
covered=missed=0
for report in reports:
    root=ET.parse(report).getroot()
    line=next(c for c in root.findall('counter') if c.attrib['type']=='LINE')
    covered+=int(line.attrib['covered']);missed+=int(line.attrib['missed'])
ratio=covered/(covered+missed) if covered+missed else 0
print(f'Backend line coverage: {ratio:.2%} ({covered}/{covered+missed})')
if ratio<0.8: raise SystemExit('Coverage gate failed: minimum 80%')
