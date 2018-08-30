import os
import glob
import fnmatch
from io import open # For Linux support.
import re


def main():

	print("Starting parser...\n")
	currentDirectory = os.path.dirname(os.path.realpath(__file__)) # Store the current location of the script.

	print("Current directory: " + currentDirectory)

	outputPath = os.path.join(currentDirectory, "Parsed files")

	if not os.path.exists(outputPath): #Create the output folder for the new xml files if it doesn't already exist.

		os.makedirs(outputPath)
		print("Created an output directory for the parsed files in: " + outputPath)

	for file in glob.glob(os.path.join(currentDirectory, "*xml")):  # For every xml file that exists in the folder.

		if not fnmatch.fnmatch(file, "*search-result-metadata.xml"): # Process all the xml files except from the
		# "search-result-metadata.xml" file that exists in the H2020 projects archive and is irrelevant.

			with open(file, "r", encoding='utf8') as sourceFile: # Open for read only. The encoding is necessary.

				# The following variables help the parser find which parts of the input file to copy to the output file.
				projectIdFound = False
				projectAcronymFound = False
				projectTitleFound = False
				projectObjective = False
				categoriesFlag = False
				deleteFile = True # Flag that indicates if the file should be deleted. It is used because there
				# are a few files (25) that do not contain <objective> and <title> tags at all. They are copies
				# of the english version of the file in different languages with missing tags!

				outputFileName = os.path.basename(sourceFile.name) # The name of the file in the output folder.
				full_path = os.path.join(outputPath, outputFileName) # The full path where the new file will be stored.
				#print("Output file name: " + outputFileName)

				with open(full_path, "w+", encoding='utf8') as targetFile: # Open for write or create the output file.

					#targetFile.readline() # Skip the first line of the xml file (header).
					targetFile.write(sourceFile.readline()) # Copy the header of the xml.
					targetFile.write(sourceFile.readline())  # Copy the project tag.

					for line in sourceFile:

						#Suppose that the id is one line-length attribute and does not cover multiple lines.
						if "<rcn>" in line and (projectIdFound is False): #If you find the id and haven't found it before.
						# This is because there are multiple ids inside an xml file.

							targetFile.write(line)
							projectIdFound = True

						#Suppose that the acronym is one line-length attribute and does not cover multiple lines.
						elif "<acronym>" in line and (projectAcronymFound is False): #If you find the acronym and haven't found it before.
						# This is because there are multiple ids inside an xml file.

							targetFile.write(line)
							projectAcronymFound = True

						# Suppose that the title is one line-length attribute and does not cover multiple lines.
						elif "<title>" in line and (projectTitleFound is False):

							targetFile.write(line)
							projectTitleFound = True

						# The objective tag might span over multiple lines!
						elif "<objective>" in line or (projectObjective is True):

							deleteFile = False  # Used for files that do not contain <objective> tag at all!

							if(not "</objective>" in line): # If the line is inside the <objectives> tag.

								projectObjective = True


							else:

								projectObjective = False

							targetFile.write(line)

						elif "    <categories>" in line or (categoriesFlag is True):
							# The correct <categories> tag we are looking for is the one that has the least empty
							# spaces before the tag. Based on the characteristics of the H2020 files, the correct
							# <categories> tag is the one that has only 4 space characters before the tag.

							if (not line.isspace()): # If the line is not empty.

								index = line.find("    <categories>")
								# Find the index where the above tag is located in the line.

								if (index == 0):
								# Find if the "     <categories" is in the first position of the line.
								# If true, then the tag is the appropriate. If not, then it is not the correct one.

									categoriesFlag = True



								#elif (not "</relations>" in line and categoriesFlag is True):
									#CATEGORIES if ("<code>") in line:
									# 	targetFile.write(line)
								# </relations> tag is the next tag after the <categories> tag, that we are looking for.
								# The above elif statement checks if we are still in the <categories> tag and if the
								# <categories> tag is the right one, because it can be found in various locations.

								else: # In any other case.

									categoriesFlag = False

						# The coordinator type in the organization tag might span over multiple lines!
						#elif "<legalName>"in line:

						#	targetFile.write(line)

						elif "<identifier>" in line:

							line = line.strip() # Remove empty spaces.
							cleanLine = re.sub('[,\.!?_-]', '', line) # Remove the spefial characters.
							targetFile.write(cleanLine+'\n') # Write the "cleaned" line to the target file.

						elif "</project>" in line:
							targetFile.write(line)

					targetFile.close() # This command is not normaly necessary. Here it is used because we might
					# need to delete the target file due to missing tags.

					if (deleteFile == True): # There are files (25) that do not have title or objective tags!!!
						os.remove(full_path)

	print("Closing parser...")

if __name__ == "__main__":
	main()
