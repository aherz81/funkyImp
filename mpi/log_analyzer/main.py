import sys


class Section:
    def __init__(self):
        self._entries = []

    def addEntry(self, line):
        self._entries.append(line)

    def getMPITime(self):
        item = self._entries[1]
        parts = item.split()
        return parts[1].strip()

    def getMessageCountAndBytes(self):
        count = 0
        size = 0
        for item in self._entries:
            parts = item.split()
            if parts[0] == "Send":
                count += int(parts[2])
                size += float(parts[3])
        return count,size

if len(sys.argv) == 1:
	print("no file provided. exiting...")
	exit()

f = open(sys.argv[1], "r")
lines = f.readlines()
f.close()

timesection = Section()
sizesection = Section()

cursection = None

for line in lines:
    if line.startswith("@--- MPI Time"):
        cursection = timesection
        continue
    elif line.startswith("@--- Aggregate Sent Message Size"):
        cursection = sizesection
        continue
    elif line.startswith("@"):
        cursection = None
        continue
    elif line.startswith("-"):
        continue

    if cursection != None:
        cursection.addEntry(line)

#analyze time
print("Time " + timesection.getMPITime() + " seconds")
#analyze sent bytes
count,size = sizesection.getMessageCountAndBytes()
print("Count " + str(count) + " messages")
print("Size " + str(size) + " B, " + str(size/1024) + " kB, " + str(size/1024/1024) + " MB")