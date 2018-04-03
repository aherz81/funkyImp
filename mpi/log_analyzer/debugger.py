

file = open("/home/andreas/funkyImp/funkyimp/input/current/tmp/mpi/orig/100/output.txt", "r")
lines = file.readlines()

file.close()

stacks = {}
for line in lines:
    if line.startswith("before"):
        if "before" not in stacks:
            stacks["before"] = 0
        else:
            stacks["before"] = stacks["before"] + 1
    elif line.startswith("after"):
        stacks["before"] = stacks["before"] - 1

    if line.startswith("bparams"):
        if "bparams" not in stacks:
            stacks["bparams"] = 0
        else:
            stacks["bparams"] = stacks["bparams"] + 1
    elif line.startswith("aparams"):
        stacks["bparams"] = stacks["bparams"] - 1

    if line.startswith("bt1"):
        if "bt1" not in stacks:
            stacks["bt1"] = 0
        else:
            stacks["bt1"] = stacks["bt1"] + 1
    elif line.startswith("at1"):
        stacks["bt1"] = stacks["bt1"] - 1

    if line.startswith("bt2"):
        if "bt2" not in stacks:
            stacks["bt2"] = 0
        else:
            stacks["bt2"] = stacks["bt2"] + 1
    elif line.startswith("at2"):
        stacks["bt2"] = stacks["bt2"] - 1

    if line.startswith("bt3"):
        if "bt3" not in stacks:
            stacks["bt3"] = 0
        else:
            stacks["bt3"] = stacks["bt3"] + 1
    elif line.startswith("at3"):
        stacks["bt3"] = stacks["bt3"] - 1


    if line.startswith("breturn"):
        if "breturn" not in stacks:
            stacks["breturn"] = 0
        else:
            stacks["breturn"] = stacks["breturn"] + 1
    elif line.startswith("areturn"):
        stacks["breturn"] = stacks["breturn"] - 1

    if line.startswith("bbuffer"):
        if "bbuffer" not in stacks:
            stacks["bbuffer"] = 0
        else:
            stacks["bbuffer"] = stacks["bbuffer"] + 1
    elif line.startswith("ebuffer"):
        stacks["bbuffer"] = stacks["bbuffer"] - 1

    if line.startswith("barray"):
        if "barray" not in stacks:
            stacks["barray"] = 0
        else:
            stacks["barray"] = stacks["barray"] + 1
    elif line.startswith("earray"):
        stacks["barray"] = stacks["barray"] - 1


    if line.startswith("calculating"):
        if "calculating" not in stacks:
            stacks["calculating"] = 0
        else:
            stacks["calculating"] = stacks["calculating"] + 1
    elif line.startswith("calculated"):
        stacks["calculating"] = stacks["calculating"] - 1

    if line.startswith("making"):
        if "making" not in stacks:
            stacks["making"] = 0
        else:
            stacks["making"] = stacks["making"] + 1
    elif line.startswith("made"):
        stacks["making"] = stacks["making"] - 1

    parts = line.split("(")
    if len(parts) > 1:
        if parts[0] == "Task":
            key = parts[1].split(")")[0]
            if key not in stacks:
                stacks[key] = 0
            else:
                stacks[key] = stacks[key] + 1
        elif parts[0] == "ExitTask":
            key = parts[1].split(")")[0]
            stacks[key] = stacks[key] - 1

for entry in stacks.keys():
    print(entry + "\t" + str(stacks[entry]))

