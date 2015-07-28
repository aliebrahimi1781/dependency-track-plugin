# coding=utf-8

import sys
import os.path

sys.path.append(os.path.join(os.path.dirname(__file__), '../Components'))
# sys.path.append('../Components')

from functions import *
# PyCharm here considers a "unresolved reference ", but it is mistaken.

input = "official-cpe-dictionary_v2.3.xml"
output = "CPE.json"

f = openFile(input, 'r')

CPE_all = {}

for line in f:
    # line = line.encode("utf-8")
    cpe = getPattern(line, '\"cpe:/a:.+\"')

    if cpe:
        VendorProductVersion = splitVendorProductVersion(cpe)

        product = VendorProductVersion[1]

        if not product in CPE_all:
            CPE_all[product] = []

        CPE_all[product].append(VendorProductVersion)

f.close()

save(output, CPE_all)