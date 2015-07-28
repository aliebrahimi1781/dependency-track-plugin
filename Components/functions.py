# coding=utf-8

import json
import sys
import re
import subprocess
import os


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


# Don't forget to close the file !!
def openFile(file, mode):
    try:
        f = open(file, mode)
    except IOError as e:
        print "{} : IOError ({}) : {}".format(e.filename, e.errno, e.strerror)
    except:
        print "Unexpected error : {}\nThis script stopped.".format(sys.exc_info()[0])
    else:
        return f


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getObjectFromJson(file):
    f = openFile(file, 'r')

    obj = json.loads(f.read())

    f.close()

    if obj:
        return obj
    else:
        return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def save(filename, obj):
    if obj:
        s = json.dumps(obj)
        try:
            with open(filename, 'w') as f:
                f.write(s)
        except IOError as e:
            print "{} : IOError ({}) : {}".format(e.filename, e.errno, e.strerror)
            return False
        except:
            print "Unexpected error : {}\nThis script stopped.".format(sys.exc_info()[0])
            return False
        else:
            return True


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getPattern(line, pattern):
    if line and pattern:
        search = re.search(pattern, line, re.IGNORECASE)
        if search:
            return search.group(0)
        else:
            return None
    else:
        return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def splitVendorProductVersion(cpe):
    if cpe:
        l = cpe.replace('"', '').split(":")

        final = []
        lenght = len(l)
        if lenght >= 5:
            for i in range(2, 5):
                final.append(l[i])
            return final
        elif lenght >= 4:
            for i in range(2, 4):
                final.append(l[i])
            final.append("")
            return final
        else:
            return None
    return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def executeCommand(cmd):
    try:
        with open(os.devnull, 'wb', 0) as DEVNULL:
            ret = subprocess.check_output(cmd, stderr=DEVNULL, shell=True)
    except subprocess.CalledProcessError as e:
        # print "{} : CalledProcessError ({})".format(e.cmd, e.returncode)
        return None
    except:
        return None
    else:
        return ret


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getPacket(lib):
    cmd = 'apt-file search {} | grep "{}$" | sed -n 1p'.format(lib, lib)
    ret = executeCommand(cmd)
    if ret:
        ret = ret.split(":")[0]
        if ret:
            if ret == "libc6":
                ret = "glibc"
            return ret
    return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getLibsOfExecutable(executablePath):
    ret = executeCommand("ldd {}".format(executablePath))

    if ret:
        libs = []

        l = ret.split("\n")
        for line in l:
            m = line.split("=>")
            if m and len(m) == 2:
                s = m[1]
                if re.search(r'\.so', s):
                    lib = s.split("(")[0]
                    libs.append(lib.strip())

        return libs

    else:
        return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getPacketsOfExecutable(executablePath):
    libs = getLibsOfExecutable(executablePath)

    if libs:
        packets = []

        for lib in libs:
            cmd = 'apt-file search {} | grep "{}$" | sed -n 1p'.format(lib, lib)
            ret = executeCommand(cmd)

            if ret:
                packet = ret.split("\n")[0].split(":")[0].strip()

                if packet and not packet in packets:
                    packets.append(packet.lower())
        return packets

    else:
        return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getVendorProductVersionOfProductAndVersion(ProductVersion):
    if ProductVersion and isinstance(ProductVersion, list) and len(ProductVersion) == 2 \
            and ProductVersion[0]:

        product = ProductVersion[0].lower()
        version = ProductVersion[1]
        vendor = ""

        CPE_dictionary = getObjectFromJson("CPE/CPE.json")

        # libpangoft2-1.0-0
        if product in CPE_dictionary:
            l = CPE_dictionary[product]
            vendor = l[0][0]
            for m in l:
                if m[2] == version:
                    return m

        # libpangoft2-1.0
        productTmp = re.split("-[0-9]+$", product)[0]
        if productTmp in CPE_dictionary:
            l = CPE_dictionary[productTmp]
            vendor = l[0][0]
            for m in l:
                if m[2] == version:
                    return m

        # libpangoft2-1
        productTmp = re.split("\.[0-9]+$", productTmp)[0]
        if productTmp in CPE_dictionary:
            l = CPE_dictionary[productTmp]
            vendor = l[0][0]
            for m in l:
                if m[2] == version:
                    return m

        # libpangoft2
        productTmp = re.split("-[0-9]+$", productTmp)[0]
        if productTmp in CPE_dictionary:
            l = CPE_dictionary[productTmp]
            vendor = l[0][0]
            for m in l:
                if m[2] == version:
                    return m

        # libpangoft
        productTmp = re.split("[0-9]+$", productTmp)[0]
        if productTmp in CPE_dictionary:
            l = CPE_dictionary[productTmp]
            vendor = l[0][0]
            for m in l:
                if m[2] == version:
                    return m


        if not vendor:
            vendor = getVendorManually(product)

        return [vendor, product, version]

    else:
        return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getAllVendorProductVersionOfExecutable(executablePath):
    AllVendorProductVersion = []
    allProduct = []

    packets = getPacketsOfExecutable(executablePath)

    if packets:
        for packet in packets:

            product = getProductOfPacket(packet)
            product = realProductName(product)

            if not product in allProduct:
                allProduct.append(product)

                version = getVersionOfPacket(packet, product)

                VendorProductVersion = getVendorProductVersionOfProductAndVersion([product, version])

                license = getLicense(product,packet)

                VendorProductVersion.append("C")
                VendorProductVersion.append(license)

                if VendorProductVersion:
                    AllVendorProductVersion.append(VendorProductVersion)

    return AllVendorProductVersion


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def cmpList(l, m):
    if not l or not m or not len(l) == len(m):
        return None
    else:
        for i in range(0, len(l)):
            if not l[i] == m[i]:
                return False
        return True


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def isListedCPE(VendorProductVersion, all_CPE):
    if not VendorProductVersion or not all_CPE:
        return None
    else:

        product = VendorProductVersion[1]

        if not product in all_CPE:
            return False
        else:
            cpes = all_CPE[product]
            for cpe in cpes:
                if cmpList(VendorProductVersion, cpe):
                    return True

            return False


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def appendListedCPE(VendorProductVersion, all_CPE):
    if not VendorProductVersion or not all_CPE:
        return None
    else:
        product = VendorProductVersion[1]

        if not product in all_CPE:
            all_CPE[product] = []

        all_CPE[product].append(VendorProductVersion)

        return all_CPE


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def reducePacket(packet):
    packet = re.split("-[0-9]", packet)[0]
    packet = re.split("\.[0-9]", packet)[0]
    # packet = re.split("[0-9]+$", packet)[0]

    return packet


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def realProductName(productTotest):
    f = openFile("realProductsNames.txt", 'r')
    for line in f:
        l = line.split(":")
        if len(l) == 2:
            product = l[0].strip()
            name = l[1].strip().replace("\n", "")

            if productTotest == product:
                return name

    f.close()

    return productTotest


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def realFormatVersion(productToTest, version):
    f = openFile("realFormatsVersions.txt", 'r')
    for line in f:
        l = line.split(":")
        if len(l) == 2:
            product = l[0].strip()
            pattern = l[1].strip().replace("\n", "")

            if productToTest == product:
                return "{}{}".format(pattern, version)

    f.close()

    return version


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getVendorManually(productToTest):
    f = openFile("vendors.txt", 'r')
    for line in f:
        l = line.split(":")
        if len(l) == 2:
            product = l[0].strip()
            vendor = l[1].strip().replace("\n", "")

            if productToTest == product:
                return vendor

    f.close()

    return ""


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getLicenseManually(productToTest):
    f = openFile("licenses.txt", 'r')
    for line in f:
        l = line.split(":")
        if len(l) == 2:
            product = l[0].strip()
            license = l[1].strip().replace("\n", "")

            if productToTest == product:
                return license

    f.close()

    return ""


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def removeFinalNumbers(str):
    return re.split("[0-9]+$", str)[0]


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getProductOfPacket(packet):
    ret = executeCommand("dpkg -p {} | grep Source".format(packet))

    if ret:
        product = ret.split(":")[1].strip()
        if product:
            product = re.split("\(.+\)", product)[0].strip()
            return product
        else:
            return packet
    else:
        return packet


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getVersionOfPacket(packet, product):
    ret = executeCommand("dpkg -p {} | grep Version".format(packet))  # apt-cache show ; dpkg -s # apt-cache policy

    if ret:
        version = ret.split(":")[1].strip()
        if version:
            version = version.split("+")[0].split("-")[0]
            version = realFormatVersion(product, version)
            return version
        else:
            return ""
    else:
        return ""


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def reduceFile(filepath):
    l = filepath.split("/")
    if l:
        i = len(l) - 1
        return l[i]
    else:
        return filepath


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getVersionProgram(program):

    # First attempt
    ret = executeCommand("{} --version".format(program))

    if ret:
        ret = ret.split('\n')[0]
        if ret:
            pattern = "[0-9][.-]*[0-9]*[.-]*[0-9]*"
            version = getPattern(ret, pattern)
            if version:
                return version

    # Second attempt
    ret = executeCommand("{} -version".format(program))

    if ret:
        ret = ret.split('\n')[0]
        if ret:
            pattern = "[0-9][.-]*[0-9]*[.-]*[0-9]*"
            version = getPattern(ret, pattern)
            if version:
                return version

    # Third attempt
    tmpFile = "tmpPluggin"
    cmd = "{} --version 2> {}".format(program, tmpFile)
    executeCommand(cmd)
    try:
        with open(tmpFile, 'r') as f:
            ret = f.readline()
        os.remove(tmpFile)
    except:
        return None
    else:
        if ret:
            pattern = "[0-9][.-]*[0-9]*[.-]*[0-9]*"
            version = getPattern(ret, pattern)
            if version:
                return version

    # Fourth attempt
    tmpFile = "tmpPluggin"
    cmd = "{} -version 2> {}".format(program, tmpFile)
    executeCommand(cmd)
    try:
        with open(tmpFile, 'r') as f:
            ret = f.readline()
        os.remove(tmpFile)
    except:
        return None
    else:
        if ret:
            pattern = "[0-9][.-]*[0-9]*[.-]*[0-9]*"
            version = getPattern(ret, pattern)
            if version:
                return version

    # Fifth attempt
    tmpFile = "tmpPluggin"
    cmd = "{} --version > {}".format(program, tmpFile)
    executeCommand(cmd)
    try:
        with open(tmpFile, 'r') as f:
            ret = f.readline()
        os.remove(tmpFile)
    except:
        return None
    else:
        if ret:
            pattern = "[0-9][.-]*[0-9]*[.-]*[0-9]*"
            version = getPattern(ret, pattern)
            if version:
                return version

    # Sixth attempt
    tmpFile = "tmpPluggin"
    cmd = "{} -version > {}".format(program, tmpFile)
    executeCommand(cmd)
    try:
        with open(tmpFile, 'r') as f:
            ret = f.readline()
        os.remove(tmpFile)
    except:
        return None
    else:
        if ret:
            pattern = "[0-9][.-]*[0-9]*[.-]*[0-9]*"
            version = getPattern(ret, pattern)
            if version:
                return version

    return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getCopyrightProduct(product):

    folder = "/usr/share/doc/"

    pattern1 = "^{}$".format(preparePattern(product).encode("utf-8"))
    pattern2 = "{}[.-][0-9]+".format(preparePattern(product).encode("utf-8"))
    # pattern3 = "{}[.-]+".format(preparePattern(product).encode("utf-8"))

    patterns = []
    patterns.append(pattern1)
    patterns.append(pattern2)
    # patterns.append(pattern3)


    for file in os.listdir(folder):

        for i in range (0,len(patterns)):

            if re.search(patterns[i], file, re.IGNORECASE):
                copyright = "/usr/share/doc/{}/copyright".format(file)
                # print "pattern : {}".format(i+1)
                # print "file : {}".format(file)
                # print "product : {}".format(product)
                # print "copyright : {}".format(copyright)
                return copyright

    return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getCopyrightPacket(packet):

    folder = "/usr/share/doc/"

    pattern4 = "^{}$".format(preparePattern(packet).encode("utf-8"))
    pattern5 = "{}[.-][0-9]+".format(preparePattern(packet).encode("utf-8"))
    # pattern6 = "{}[.-]+".format(preparePattern(packet).encode("utf-8"))

    patterns = []
    patterns.append(pattern4)
    patterns.append(pattern5)
    # patterns.append(pattern6)

    for file in os.listdir(folder):

        for i in range (0,len(patterns)):

            if re.search(patterns[i], file, re.IGNORECASE):
                copyright = "/usr/share/doc/{}/copyright".format(file)
                # print "pattern : {}".format(i+4)
                # print "file : {}".format(file)
                # print "packet : {}".format(packet)
                # print "copyright : {}".format(copyright)
                return copyright

    return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getCopyright(product, packet):

    copyright = getCopyrightProduct(product)

    if not copyright:
        copyright = getCopyrightPacket(packet)

    if copyright:
        return copyright
    else:
        print "No copyright file for product : {} , package : {}".format(product, packet)
        return None


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def preparePattern(str):
    if not str:
        return ""
    else:
        str = str.replace("+", "\+")
        return str


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def getLicense(product, packet):

    noLicense = "-"

    copyright = getCopyright(product, packet)

    if copyright:

        licenses = "Licenses/Licenses.json"

        try:
            licenses = open(licenses, 'r')
            copyright = open(copyright, 'r')
        except IOError as e:
            print "{} : IOError ({}) : {}".format(e.filename, e.errno, e.strerror)
        except:
            print "Unexpected error : {}\nThis script stopped.".format(sys.exc_info()[0])
        else:

            dictLicenses = json.loads(licenses.read())
            licenses.close()

            for line in copyright:

                for license in dictLicenses:
                    pattern = "[\"\'/,;:\|\s\n\t\(]{}[,;:\.\|\s\n\t\)\-\"\']+".format(license.encode("utf-8"))
                    if re.search(pattern, line, re.IGNORECASE):
                        return license

            copyright.close()

            license = getLicenseManually(product)

            if license:
                return license
            else:
                return noLicense

    else:
        return noLicense


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------


def displayProgramInfo(program_info):
    if program_info:
        print "\nAll the collected information :\n"
        for program in program_info:
            components = program_info[program][0]
            version = program_info[program][1]

            print "Program : {}".format(program)
            print "\tVersion : {}".format(version)

            for component in components:
                print "\t\tProduct : {}".format(component[1])
                print "\t\t\tVendor : {}".format(component[0])
                print "\t\t\tVersion : {}".format(component[2])
                print "\t\t\tLicense : {}".format(component[4])
                print "\t\t\tLanguage : {}".format(component[3])
        print "\nThe 'program_info.json' file with all the information, has been created. Now :\n"
        print "1) Copy this file in the '/var/opt/dependency-track-pluggin/' folder."
        print "2) Go to the Dependency-Track web interface : Settings --> Launch Plugin."
        print "\nEnjoy !\n"


# ----------------------------------------------------------------------------------------------------------------------
# ----------------------------------------------------------------------------------------------------------------------
