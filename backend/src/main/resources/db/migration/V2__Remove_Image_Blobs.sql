-- Remove image BLOB columns from tables since we now use Google Cloud Storage URLs

-- Remove 'image' BLOB column from mystery_object table
ALTER TABLE mystery_object DROP COLUMN image;

-- Remove 'file_data' BLOB column from media_file table
ALTER TABLE media_file DROP COLUMN file_data;
