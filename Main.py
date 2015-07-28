import sys
sys.path.append('Components')
from functions import *

input = "executables.txt"
output = "dependency-track/program_info.json"

program_info = {}

f = openFile(input,'r')

for line in f:
    program = line.strip().replace("\n","")

    if line:

        print "{} is analyzing. Wait please...".format(program)

        # Getting components
        components = getAllVendorProductVersionOfExecutable(program)

        if components:

            # Getting version
            version = getVersionProgram(program)
            if not version:
                version= '0'

            # Saving components and version of the program
            program = reduceFile(program)
            program_info[program] = []
            program_info[program].append(components)
            program_info[program].append(version)

f.close()

# Saving
save(output,program_info)

displayProgramInfo(program_info)
