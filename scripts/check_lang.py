#!/usr/bin/env python3
"""Language file checker of ICBM-Classic (i18n.md §8; skeleton from S2.10, finished in S8.03).

Usage:
    python3 scripts/check_lang.py                # check, exit code 1 on error
    python3 scripts/check_lang.py --dump-en      # print the merged English key set (JSON)
    python3 scripts/check_lang.py --fix-order    # rewrite every hand written language file sorted by key
    python3 scripts/check_lang.py --report       # per language: keys, missing, extra, placeholder mismatches
    python3 scripts/check_lang.py --strict       # treat check 4 (required languages) as an error

Checks:
    1. every file is a valid JSON object, all values are strings, UTF-8 without BOM
    2. the merged en_us has no empty values
    3. the keys of every other language are a subset of en_us (extra keys are an error)
    4. required languages (zh_cn fr_fr de_de ja_jp ar_sa) must have every key (warning until S8, error with
       --strict)
    5. the placeholder index sets match the English ones
    6. no '§' colour code
    7. every literal key of ``Component.translatable("...")`` in src/**/*.java exists in en_us; a literal
       that ends with '.' is a prefix of a built key (``"icbmclassic.tier." + name``) and has to match at
       least one key

Only the Python standard library is used.
"""

import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESOURCE_LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "icbmclassic", "lang")
GENERATED_LANG = os.path.join(ROOT, "src", "main", "generated", "assets", "icbmclassic", "lang")
I18N_DIR = os.path.join(ROOT, "src", "i18n")
EN_US = "en_us.json"

REQUIRED_LANGUAGES = ("zh_cn", "fr_fr", "de_de", "ja_jp", "ar_sa")
PLACEHOLDER = re.compile(r"%(\d+\$)?[sdf]")
TRANSLATABLE = re.compile(r'Component\.translatable\(\s*"([^"]+)"')
# Keys that Minecraft itself always provides; the mod does not need to ship them (S5.11)
EXTERNAL_TRANSLATION_KEYS = {"container.inventory"}

errors = []
warnings = []


def error(message):
    errors.append(message)


def warn(message):
    warnings.append(message)


def load_json(path):
    """Check 1: UTF-8 without BOM, JSON object, string values only."""
    with io.open(path, "rb") as stream:
        raw = stream.read()
    if raw.startswith(b"\xef\xbb\xbf"):
        error("%s: has a UTF-8 BOM" % path)
        raw = raw[3:]
    try:
        data = json.loads(raw.decode("utf-8"))
    except Exception as exc:
        error("%s: not valid UTF-8 JSON (%s)" % (path, exc))
        return {}
    if not isinstance(data, dict):
        error("%s: the top level is not a JSON object" % path)
        return {}
    for key, value in data.items():
        if not isinstance(value, str):
            error("%s: value of %s is not a string" % (path, key))
    return data


def language_files(directory):
    """``{language: path}`` of the .json files in a lang directory."""
    if not os.path.isdir(directory):
        return {}
    return {name[:-5]: os.path.join(directory, name)
            for name in sorted(os.listdir(directory)) if name.endswith(".json")}


def merged_english():
    """Merged en_us of both sources (i18n.md §4); a key with two different values is a conflict."""
    merged = {}
    sources = []
    generated = os.path.join(GENERATED_LANG, EN_US)
    handwritten = os.path.join(RESOURCE_LANG, EN_US)
    for path in (generated, handwritten):
        if os.path.isfile(path):
            sources.append(path)
        else:
            warn("%s does not exist yet" % path)
    for path in sources:
        for key, value in load_json(path).items():
            if key in merged and merged[key] != value:
                error("lang key conflict: %s (%s vs %s)" % (key, path, "another source"))
            merged[key] = value
    return merged


def placeholders(value):
    """Check 5: the index set of the placeholders, ``%s`` gets the next free index (i18n.md §3)."""
    indexes = set()
    next_index = 1
    for match in PLACEHOLDER.finditer(value):
        explicit = match.group(1)
        if explicit:
            index = int(explicit[:-1])
        else:
            index = next_index
            next_index += 1
        indexes.add(index)
        next_index = max(next_index, index + 1)
    return indexes


def check_file(language, path, english):
    """Checks 2, 3, 5 and 6 for one language file; returns the counts used by --report."""
    data = load_json(path)
    missing = sorted(key for key in english if key not in data)
    extra = sorted(key for key in data if key not in english)
    empty = sorted(key for key, value in data.items() if not value.strip())
    colours = sorted(key for key, value in data.items() if "\u00a7" in value)
    placeholder = sorted(key for key, value in data.items()
                         if key in english and placeholders(value) != placeholders(english[key]))

    if language == "en_us":
        for key in sorted(key for key, value in english.items() if not value.strip()):
            error("en_us: %s is empty" % key)
    else:
        if extra:
            error("%s: %d keys are not in en_us: %s" % (language, len(extra), ", ".join(extra[:10])))
    for key in empty:
        error("%s: %s is empty" % (language, key))
    for key in colours:
        error("%s: %s contains a colour code (i18n.md §3)" % (language, key))
    for key in placeholder:
        error("%s: %s has different placeholders than en_us (%s vs %s)"
              % (language, key, sorted(placeholders(data[key])), sorted(placeholders(english[key]))))
    if language in REQUIRED_LANGUAGES and missing:
        message = "%s: %d required keys are missing" % (language, len(missing))
        if strict:
            error(message)
        else:
            warn(message)
    return {"keys": len(data), "missing": len(missing), "extra": len(extra), "placeholder": len(placeholder)}


def check_code_keys(english):
    """Check 7: literal Component.translatable keys of the ported code must exist in en_us."""
    for base in ("src/main/java", "src/client/java", "src/gametest/java", "src/test/java"):
        directory = os.path.join(ROOT, base)
        for directory_name, _, files in os.walk(directory):
            for name in files:
                if not name.endswith(".java"):
                    continue
                path = os.path.join(directory_name, name)
                with io.open(path, encoding="utf-8") as stream:
                    for number, line in enumerate(stream, start=1):
                        for key in TRANSLATABLE.findall(line):
                            if key in english or key in EXTERNAL_TRANSLATION_KEYS:
                                continue
                            if key.endswith(".") and any(k.startswith(key) for k in english):
                                continue
                            error("%s:%d: %s is not in en_us" % (os.path.relpath(path, ROOT), number, key))


def fix_order(english):
    """--fix-order: rewrite the hand written files with sorted keys."""
    for language, path in language_files(RESOURCE_LANG).items():
        data = load_json(path)
        with io.open(path, "w", encoding="utf-8") as stream:
            stream.write(json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n")
    # the Arabic source file is part of the language files as well (i18n.md §5)
    source = os.path.join(I18N_DIR, "ar_sa.source.json")
    if os.path.isfile(source):
        data = load_json(source)
        with io.open(source, "w", encoding="utf-8") as stream:
            stream.write(json.dumps(data, indent=2, ensure_ascii=False, sort_keys=True) + "\n")


def main(argv):
    global strict
    strict = "--strict" in argv

    english = merged_english()
    if "--dump-en" in argv:
        print(json.dumps(english, indent=2, ensure_ascii=False, sort_keys=True))
        return 0
    if "--fix-order" in argv:
        fix_order(english)
        return 0

    for key in sorted(key for key, value in english.items() if not value.strip()):
        error("en_us: %s is empty" % key)

    report = {}
    for language, path in language_files(RESOURCE_LANG).items():
        if language == "en_us":
            continue
        report[language] = check_file(language, path, english)
    for language, path in language_files(I18N_DIR).items():
        if language.endswith(".source"):
            report[language] = check_file(language, path, english)
    report["en_us"] = {"keys": len(english), "missing": 0, "extra": 0, "placeholder": 0}

    check_code_keys(english)

    if "--report" in argv:
        for language in sorted(report):
            counts = report[language]
            print("%-12s keys=%-5d missing=%-5d extra=%-5d placeholders=%d"
                  % (language, counts["keys"], counts["missing"], counts["extra"], counts["placeholder"]))
    for message in warnings:
        print("warning: %s" % message)
    for message in errors:
        print("error: %s" % message)
    if not errors:
        print("lang ok: %d keys in en_us" % len(english))
    return 1 if errors else 0


strict = False

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
