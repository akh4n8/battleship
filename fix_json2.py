import re
with open('app/src/commonMain/kotlin/com/ak/battleship/viewmodel/BattleshipViewModel.kt', 'r') as f:
    text = f.read()

bad_list = [
    '"{"turn":${m.turnNumber},"x":${m.x},"y":${m.y},"result":"${m.result}","is_offense":${m.isOffense}}"',
    '"{{\\"turn\\":${m.turnNumber},\\"x\\":${m.x},\\"y\\":${m.y},\\"result\\":\\"${m.result}\\",\\"is_offense\\":${m.isOffense}}}"'
]

good = '"""{"turn":${m.turnNumber},"x":${m.x},"y":${m.y},"result":"${m.result}","is_offense":${m.isOffense}}"""'

for bad in bad_list:
    text = text.replace(bad, good)

with open('app/src/commonMain/kotlin/com/ak/battleship/viewmodel/BattleshipViewModel.kt', 'w') as f:
    f.write(text)
