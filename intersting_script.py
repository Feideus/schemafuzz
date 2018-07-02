import argparse
import subprocess
import csv
import os
import sys
import shutil
import xlsxwriter
import codecs
import re
reload(sys)
sys.setdefaultencoding('utf8')
global_keep_separate_output_files = False
global_input_is_directory = False
import xlrd
import csv
"""
Simple function to convert xlsx to csv used by this program
internally.
"""
def csv_from_excel():
wb = xlrd.open_workbook('Crashes.xlsx')
2
sh = wb.sheet_by_name('Sheet1')
your_csv_file = open('Crashes.csv', 'wb')
wr = csv.writer(your_csv_file, quoting=csv.QUOTE_ALL)
for rownum in xrange(sh.nrows):
wr.writerow(sh.row_values(rownum))
your_csv_file.close()
"""
Simple function to verify args.
- Checks that fuzzed program, input directory
and output directories exists.
"""
def Validate_args(args):
print ('\n')
print ('Checking args')
parser = argparse.ArgumentParser(description='Processes American
Fuzzy Lop logs with GDB and creates a
database of the results.')
parser.add_argument('fuzzed', metavar='program',
help='the fuzzed program')
parser.add_argument('input', metavar='input',
help='input directory of crashes')
parser.add_argument('output', metavar='output',
help='output directory to store analyzed crashes')
parser.add_argument('keep', metavar='keep',
help='if set to True keeps separate output files
instead of deleting them on exit.')
args = parser.parse_args()3
#print(args)
if (os.path.lexists(args.fuzzed) == False):
print('The fuzzed program could not be found')
sys.exit()
if (os.path.lexists(args.input) == False):
print('The input directory could not be found')
sys.exit()
if (os.path.isdir(args.input) == True):
print(os.listdir(args.input))
list = os.listdir(args.input)
number_files = len(list)
print number_files
global_input_is_directory = True
if (os.path.lexists(args.output) == False):
print('The AFL\'s output file or directory could not be
found')
sys.exit()
if (os.path.isdir(args.output) == False):
print('The AFL\'s output is not directory')
sys.exit()
if(len(os.listdir(args.output)) >0):
print('The AFL\'s output directory still has some files!
Remove those.')4
sys.exit()
keep_files = args.keep[0]
keep_files = keep_files.upper()
if (keep_files == 'Y'):
print('Leaving separate output files')
global_keep_separate_output_files = True
else:
print('Not keeping separate output files')
print ('\n')
Validate_args(args)
print ('\n')
#Reads the number of files in the input directory
list = os.listdir(args.input)
number_files = len(list)
number_files = int(number_files)
path_to_input_files = os.getcwd() + '\\' + args.input + '\\'
#Copies the given fuzzed program to the same directory as the input
files in order to enable GDB to process it in batch run.
shutil.copy(args.fuzzed, args.input + '/' + args.fuzzed)
#Limits the files to be processed by GDB to the files that start with
id_
inputFilesList = [x for x in list if x.startswith('id:')]5
#Processes every file in inputFilesList and runs GDB as a shell
command for every input files
amountOfFiles = len(inputFilesList)
prevdir = os.getcwd()
for i in range(amountOfFiles):
ifile = open(args.input + '//run_gdb.bat', "wb+")
os.chmod(args.input + '//run_gdb.bat', 0o777)
filename=inputFilesList[i]
#print(inputFilesList[i])
parameters2 = "gdb --batch -ex 'r <"+filename+"'
"+args.fuzzed+" -ex bt >output_"+filename+".txt"
ifile.write(parameters2)
ifile.close()
print ('\nSending file: ' + filename + ' to GDB')
os.chdir(args.input)
subprocess.call('./run_gdb.bat', shell=True)
os.chdir(prevdir)
#print parameters2
#Before writing to a database file every output file from GDB is
moved to output directory
source =os.getcwd() + '/' + args.input
destination =
os.getcwd() + '/' + args.output
files = os.listdir(source)
prevdir = os.getcwd()6
os.chdir(args.input)
for f in files:
if (f.endswith("txt")):
shutil.move(f, destination)
os.chdir(prevdir)
list = os.listdir(args.output)
number_files = len(list)
number_files = int(number_files)
path_to_output_files = os.getcwd() + '/' + args.output + '/'
# A workbook is created and a worksheet is added.
workbook = xlsxwriter.Workbook('Crashes.xlsx')
worksheet = workbook.add_worksheet()
# Start from the first cell. Rows and columns are zero indexed.
row = 0
col = 0
print ('\nStarting to write output files:')
for i in range(len(list)):
#ifile = open(path_to_output_files + list[i], "rb")
ifile = codecs.open(path_to_output_files + list[i], "rb",
encoding='utf8', errors = 'ignore')
print ("\tWriting output file: " + list[i])
reader = ifile.readlines()7
#print reader
x = i+1
worksheet.write_string(row, col, 'crash dump #:' + str(x))
row += 1
for item in (reader):
print item
item = re.sub(r'\([^()]*\)', '', item)
#worksheet.write_string(row, col, item)
worksheet.write(row, col, item)
row += 1
worksheet.write_blank(row, col, item)
workbook.close()
numberOfInputFiles = str(len(inputFilesList))
numberOfOutputFiles = str(len(os.listdir(path_to_output_files)))
print ('\nProducing output files, please wait...')
csv_from_excel()
print "\n" + numberOfInputFiles +
" input files processed and " +
numberOfOutputFiles + " output files done!"
#clean up
if (keep_files == 'N'):
print('\nRemoving separate output files')
os.chdir(args.output)
files = os.listdir(path_to_output_files)
for f in files:
if (f.endswith("txt") and len(f) >1):8
os.remove(f)