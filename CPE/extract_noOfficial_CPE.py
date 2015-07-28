# coding=utf-8

import os
import sys
sys.path.append('../Components')
from functions import *
# PyCharm here considers a "unresolved reference ", but it is mistaken.

folder = "CVE/"
output = "CPE.json"
input = "CPE.json"

all_CPE = getObjectFromJson(input)


for file in os.listdir(folder):

    filename = "{}{}".format(folder, file)
    print "{} is parsing. Wait please...".format(filename)

    f = openFile(filename,'r')

    for line in f:

        cpe = getPattern(line, '\"cpe:/a:.+\"')

        if cpe:
            VendorProductVersion = splitVendorProductVersion(cpe)

            if not isListedCPE(VendorProductVersion,all_CPE):
                all_CPE = appendListedCPE(VendorProductVersion,all_CPE)
                # print "{} is appended in the CPE dictionary".format(cpe)

    f.close()


save(output, all_CPE)

