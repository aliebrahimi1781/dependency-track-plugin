__author__ = 'flo'

import sys
from functions import *


def getArguments(arguments):
    dict = {}

    if arguments:
        nbrArguments = len(arguments)
        if nbrArguments > 1 :

            for i in range(1, nbrArguments):
                array = arguments[i].split("=")

                if len(array) > 1 :
                    option = array[0].strip()
                    value = array[1].strip()

                    if option and value :
                        dict[option]= value

    return dict

def displayCPEinfo(dict,v):
    if dict:

        products = set()
        vendors = set()

        print "\nAll collected information :\n"
        for p in dict:
            for l in dict[p]:

                vendor = l[0]
                product = l[1]
                version = l[2]

                if (not v) or (v and v==version):
                    products.add(product)
                    vendors.add(vendor)

                    print "Vendor : {}".format(vendor)
                    print "\tProduct : {}".format(product)
                    print "\t\tVersion : {}\n".format(version)

        print "\nAll product(s) found : "
        for s in products:
            print "\t{}".format(s)
        print "\nAll vendor(s) found : "
        for s in vendors:
            print "\t{}".format(s)
        print""

    else:
        print "No information found in CPE database"


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


if sys.argv:
    arguments = sys.argv
    nbrArguments = len(arguments)
    nameScript = arguments[0]

    if nbrArguments <= 1:
        print "Usage : python2.7 Components/{} p=product [v=version]".format(nameScript)

    else:  # nbrArguments >= 2
        dictArguments = getArguments(arguments)

        if "p" in dictArguments :

            # product = package or source ?
            p = dictArguments["p"]
            product = getProductOfPacket(p)

            if product :
                print product
                product = realProductName(product)

            version = ""
            if "v" in dictArguments:
                version = dictArguments["v"]

            CPEinfo = getVendorProductVersionOfProduct([p,product])

            displayCPEinfo(CPEinfo,version)








