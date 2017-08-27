# This util was written to aid the build system and extracts a package version 
# from a file. 
#
# The file must contain the a line of text as
# VERSION=X 
# where X is a number. The number will always be presented as a float value. 
# Therfore integers will be converted to floats.

import sys
import os

def fatal(msg):
  print 'ERROR: %s' % (msg)
  sys.exit(-1)
  
if __name__ == "__main__":
  foundVersion=0
  
  if len(sys.argv) < 2:
    fatal('You must supply the text file to extract the version from on the command line.')
    
  ver_file = sys.argv[1]
  if not os.path.isfile(ver_file):
    fatal("%s file not found." % (ver_file) )
    
  fd = open(ver_file)
  lines = fd.readlines() 
  fd.close()
  
  for l in lines:
    l=l.strip()
    l=l.lower()
    if l.startswith('public static final double netplot_version'):
      elems = l.split('=')
      if len(elems) == 2:
        vs = elems[1]
        vs = vs.replace(';','')
        vs = vs.strip()
        vs = vs.rstrip('\r')
        vs = vs.rstrip('\n')
        try:
          version=float(vs)
          foundVersion=foundVersion+1
          print vs
        except ValueError:
          raise
          fatal("Failed to extract version number from line: %s" % (l) )

  if foundVersion == 0:
    fatal("Failed to extract version from %s" % (ver_file) )
    
  if foundVersion > 1:
    fatal("More than one occurance of version=X was found in %s" % (ver_file) )
    
