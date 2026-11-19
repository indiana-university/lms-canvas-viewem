package edu.iu.uits.lms.viewem.service;

/*-
 * #%L
 * lms-canvas-viewem
 * %%
 * Copyright (C) 2015 - 2026 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.iu.uits.lms.viewem.controller.DataTooBigException;
import edu.iu.uits.lms.viewem.model.PendingUpload;
import edu.iu.uits.lms.viewem.model.Sheet;
import edu.iu.uits.lms.viewem.model.SheetColumn;
import edu.iu.uits.lms.viewem.model.SheetUser;
import edu.iu.uits.lms.viewem.model.UserData;
import edu.iu.uits.lms.viewem.repository.SheetRepository;
import edu.iu.uits.lms.viewem.repository.SheetUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by chmaurer on 6/12/15.
 */
@Service
@Slf4j
public class ViewemService {
    @Autowired
    private SheetRepository sheetRepository = null;
    
    @Autowired
    private SheetUserRepository sheetUserRepository = null;

    @Autowired
    private ResourceBundleMessageSource messageSource = null;

    /**
     * Parse a pending upload (stored by the Preview step) again and save it. For an edit-via-upload, the result
     * keeps the existing sheet's id, so saving replaces that sheet's columns and users.
     * @param pending The upload stored at Preview
     * @param title Title for the sheet
     * @param context Context where the sheet lives
     * @param systemId System identifier where the sheet lives
     * @param owner User id of the person saving
     * @param published New published flag, or null to keep the existing sheet's flag (unpublished for a new sheet)
     * @param validUserIds Roster user ids; rows for anyone else are skipped, as at Preview
     * @return the saved Sheet
     * @throws FileUploadException if the pending upload can't be read or parsed
     * @throws DataTooBigException if some of the data is too big for the database
     */
    public Sheet savePendingUpload(PendingUpload pending, String title, String context, String systemId, String owner,
                                   Boolean published, List<String> validUserIds) throws FileUploadException, DataTooBigException {
        boolean basePublished = false;
        if (pending.sheetId() != null) {
            basePublished = sheetRepository.findById(pending.sheetId()).map(Sheet::isPublished).orElse(false);
        }

        Sheet sheet;
        try (InputStream csv = pending.csvStream()) {
            // preview=true: build the Sheet without saving it, so the edit id and publish flag can be applied first
            sheet = processCsvFile(csv, title, context, systemId, basePublished, owner, true, validUserIds, new ArrayList<>());
        } catch (IOException e) {
            throw new FileUploadException("Unable to read pending upload", e);
        }
        if (sheet == null) {
            // processCsvFile logs and returns null when the CSV can't be read
            throw new FileUploadException("Unable to parse pending upload");
        }

        if (pending.sheetId() != null) {
            sheet.setSheetId(pending.sheetId());
        }
        if (published != null) {
            sheet.setPublished(published);
        }
        return sheetRepository.save(sheet);
    }

   /**
    * Process an uploaded csv file into a Sheet (and related objects)
    * @param csvFile Input stream of the uploaded csv file
    * @param title Title for the sheet
    * @param context Context where the sheet lives
    * @param systemId System identifier where the sheet lives (for multi-tennancy)
    * @param published Is the sheet published (visible to all)
    * @param owner User id of the owner of the sheet
    * @param preview Is this going to be a prevew of the sheet or saving it?
    * @param validUserIds List of valid userIds to compare
    * @param skippedUserIds List of userIds that were skipped from the sheet as they weren't in the list of valid ids.  Will be displayed to the end user.
    * @return a persisted Sheet object
    * @throws FileUploadException Throws exception if the file is improperly formatted (i.e. not enough rows/columns)
    * @throws DataTooBigException Throws an exception if some of the data is going to be too big for the database
    */
    public Sheet processCsvFile(InputStream csvFile, String title, String context, String systemId, boolean published,
                                String owner, boolean preview, List<String> validUserIds, List<String> skippedUserIds) throws FileUploadException, DataTooBigException {
        Sheet sheet = null;
        try (CSVReader reader = new CSVReader(new InputStreamReader(csvFile))) {
            List<String[]> contents = reader.readAll();

            if (!quickValidateContents(contents)) {
                throw new FileUploadException();
            }
            
            // set these all to lowercase for comparison later
            List<String> lowercaseValidUserIds = new ArrayList<String>();
            if (validUserIds != null) {
            	for (String validId : validUserIds) {
            		if (validId != null) {
            			lowercaseValidUserIds.add(validId.toLowerCase());
            		}
            	}
            }

            sheet = new Sheet(title, context, systemId, published, owner, owner);
            int lineCount = 0;
            for (String[] line : contents) {
                if (lineCount == 0) {
                    //header row
                    for (int i = 1; i < line.length; i++) {
                        // The limit here reflects the field size limit of column headers in the database
                        if (line[i].length() > SheetColumn.SHEET_COLUMN_TITLE_SIZE) {
                            throw new DataTooBigException();
                        }
                        SheetColumn sheetColumn = new SheetColumn(line[i]);
                        sheet.addColumn(sheetColumn);
                    }
                } else {
                    String userId = line[0];
                    if (userId == null || "".equals(userId)) {
                        throw new FileUploadException();
                    }
                    
                    // make sure the userid is case insensitive
                    userId = userId.toLowerCase();
                    
                    if (lowercaseValidUserIds.contains(userId)) {
                        SheetUser sheetUser = new SheetUser(userId);

                        for (int i = 1; i < line.length; i++) {
                            // The limit here reflects the field size limit of the data field in the database
                            if (line[i].length() > UserData.USER_DATA_DATA_SIZE) {
                                throw new DataTooBigException();
                            }
                            sheetUser.addUserData(new UserData(line[i]));
                        }
                        sheet.addSheetUser(sheetUser);
                    } else {
                        skippedUserIds.add(userId);
                    }
                }
                lineCount++;
            }
            if (!preview) {
                sheetRepository.save(sheet);
            }

        } catch (IOException | CsvException e) {
            log.error("Unable to parse csv file", e);
        }
        return sheet;
    }

    /**
     * Ensure that there are at least 2 rows and 2 columns
     * @param fileData
     * @return
     */
    private boolean quickValidateContents(List<String[]> fileData) {
        boolean validated = false;
        if (fileData != null && fileData.size() > 1) {
            if (fileData.get(0).length > 1)
                validated = true;
        }
        return validated;
    }

   /**
    * Use to validate that the file is a csv file
    * @param file Input stream of the uploaded csv file
    * @return true/false depending if the file is valid for processing
    */
    public boolean validateCsv(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(fileName);
        return "csv".equalsIgnoreCase(extension);
    }

   /**
    *
    * @param sheet
    * @return a list of String[] values representing the header and rows in the given sheet. Used
    * for downloading a csv file representing the sheet
    */
    public List<String[]> convertSheetToArray(Sheet sheet) {
    	
    	if (sheet == null) {
    		throw new IllegalArgumentException("Null sheet passed to convertSheetToArray");
    	}
    	
    	List<String[]> sheetContents = new ArrayList<String[]>();
    	
    	// first, add the headings
    	List<SheetColumn> columns = sheet.getColumns();
    	if (columns == null || columns.size() == 0) {
    		throw new IllegalArgumentException("No columns exist for the given sheet");
    	}
    	
    	List<String> header = new ArrayList<String>();
		List<List<String>> userRows = new ArrayList<List<String>>();
    
    	// Construct the header row
    	// The first column is Student ID
    	header.add(messageSource.getMessage("instructorView.header.studentId", null, Locale.getDefault()));
    	
    	for (SheetColumn column : columns) {
    		header.add(column.getTitle());
    	}
    	
    	// Construct the student rows
    	List<SheetUser> sheetUsers = sheetUserRepository.findBySheet(sheet.getSheetId());
    	if (sheetUsers != null) {	
    		for (SheetUser user : sheetUsers) {
    			List<String> row = new ArrayList<String>();
    			row.add(user.getUserId());
    			
        		List<UserData> allData = user.getData();
        		if (allData != null) {
        			for (UserData data : allData) {
        				row.add(data.getData());
        			}
        		}
        		
        		userRows.add(row);
        	}
    	}
    	
    	// convert this data to arrays
    	sheetContents.add(header.toArray(new String[header.size()]));
    	for (List<String> row : userRows) {
    		sheetContents.add(row.toArray(new String[row.size()]));
    	}
    	  	
    	return sheetContents;
    }


}
