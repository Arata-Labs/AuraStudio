# Regenerates termux-bootstrap-zip.S from the template, substituting
# @BOOTSTRAP_DIR@ with the source directory. file(TOUCH) afterwards updates the
# output mtime even when the template text is unchanged, forcing the assembler
# to re-read the embedded bootstrap archive whenever the zip content changed.
set(BOOTSTRAP_DIR "${SRC}")
configure_file("${SRC}/termux-bootstrap-zip.S.in" "${BIN}/termux-bootstrap-zip.S" @ONLY)
file(TOUCH "${BIN}/termux-bootstrap-zip.S")