import re
path = r"c:\\Users\\HDC i5 10400\\Desktop\\Aurea\\AureaTrabajoFinal\\demo\\src\\main\\java\\com\\example\\demo\\service\\ExamenService.java"
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()
# insert warning after materialesIds block
needle = "            }\n\n            // 5. GENERAR 8 PREGUNTAS CON IA del material del mÃ³dulo"
if needle in text:
    text = text.replace(needle, "            }\n            if (materialesIds.isEmpty()) {\n                log.warn(\"No hay materiales en los mÃ³dulos seleccionados. No se genera pre-examen.\");\n                return;\n            }\n\n            // 5. GENERAR 8 PREGUNTAS CON IA del material del mÃ³dulo")
else:
    raise SystemExit('needle not found')
# add oferta set
text = text.replace("poolPreExamen.setIaStatus(com.example.demo.enums.IaGenerationStatus.PENDING);",
                    "poolPreExamen.setIaStatus(com.example.demo.enums.IaGenerationStatus.PENDING);\n            poolPreExamen.setOferta(modulo.getCurso());")
# replace paramsJson block
old = """            String paramsJson = String.format(
                    \"{\\\"temas\\\": \\\"%s\\\", \\\"materialesIds\\\": [%s], \\\"cantidadPreguntas\\\": 8, \\\"contextoMaterial\\\": \\\"%s\\\", \\\"objetivos\\\": \\\"%s\\\", \\\"bibliografia\\\": \\\"%s\\\"}\",
                    escaparJson(temas),
                    idsJoiner.toString(),
                    escaparJson(modulo != null && modulo.getTemario() != null ? modulo.getTemario() : \"Contenido del mÃ³dulo\"),
                    escaparJson(modulo != null && modulo.getObjetivos() != null ? modulo.getObjetivos() : \"\"),
                    escaparJson(modulo != null && modulo.getBibliografia() != null ? modulo.getBibliografia() : \"\"));
"""
new = """            StringJoiner tiposJoiner = new StringJoiner(\"\\\",\\\"\", \"[\\\"\", \"\\\"]\");
            tiposJoiner.add(\"MULTIPLE_CHOICE\");
            tiposJoiner.add(\"VERDADERO_FALSO\");
            tiposJoiner.add(\"UNICA_RESPUESTA\");

            String paramsJson = String.format(
                    \"{\\\"temas\\\": \\\"%s\\\", \\\"materialesIds\\\": [%s], \\\"cantidad\\\": 8, \\\"tipos\\\": %s, \\\"contextoMaterial\\\": \\\"%s\\\", \\\"objetivos\\\": \\\"%s\\\", \\\"bibliografia\\\": \\\"%s\\\"}\",
                    escaparJson(temas),
                    idsJoiner.toString(),
                    tiposJoiner.toString(),
                    escaparJson(modulo != null && modulo.getTemario() != null ? modulo.getTemario() : \"Contenido del mÃ³dulo\"),
                    escaparJson(modulo != null && modulo.getObjetivos() != null ? modulo.getObjetivos() : \"\"),
                    escaparJson(modulo != null && modulo.getBibliografia() != null ? modulo.getBibliografia() : \"\"));
"""
if old not in text:
    raise SystemExit('params block not found')
text = text.replace(old, new)
with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
print('updated')
