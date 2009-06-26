#! /usr/bin/python

import sys

if len(sys.argv) <= 2:
	print >> sys.stderr, "USAGE:\n\t%s <filename> <headerfile>" % sys.argv[0]
	raise SystemExit

f = file(sys.argv[1])
content = "".join(f.readlines())
f.close()

f = file(sys.argv[2])
new_header = "".join(f.readlines())
f.close()


# When we insert the new header we do it overriding the
# data in position [0:end_decl]
end_decl = 0

# If the file contents starts with '/*' it is probably an LGPL declaration,
# so we verify that this is the case 
if content.startswith("/*"):
	end_decl = content.find("*/")
	
	if end_decl == -1:
		print >> sys.stderr, "No matching end declaration"
		raise SystemExit		
	
	end_decl += 2
else:
	# There is no file header, so we insert at the file top
	end_decl = 0
	
if end_decl != 0 and not "GNU" in content[0:end_decl]:
		print >> sys.stderr, "File header does not look like an LGPL header"
		raise SystemExit		

patched_content = content[end_decl:]
print new_header + patched_content
	
	
