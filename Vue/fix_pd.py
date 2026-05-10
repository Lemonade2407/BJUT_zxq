path = "src/components/ProjectDetail.vue"
with open(path, encoding="utf-8") as f:
    content = f.read()

old_exp = "// " + chr(25991) + chr(20214) + chr(22841) + chr(23637) + chr(24320) + chr(29376) + chr(24577) + "\nconst expandedFolders = ref(new Set())\n"
content = content.replace(old_exp, "", 1)

start = content.find("// " + chr(25353) + chr(25991) + chr(20214) + chr(22841) + chr(32467) + chr(26500) + chr(32452) + chr(32455) + chr(25991) + chr(20214) + "\n")
end = content.find("// " + chr(22788) + chr(29702) + chr(25991) + chr(20214) + chr(36873) + chr(25321) + "\nconst handleFileSelect")

if start >= 0 and end >= 0:
    repl = "const { organizedFiles, displayFiles, toggleFolder, expandedFolders } = useFileTree(selectedFiles)\n\n"
    content = content[:start] + repl + content[end:]
    print("Replaced file tree block")
else:
    print(f"start={start} end={end}")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
