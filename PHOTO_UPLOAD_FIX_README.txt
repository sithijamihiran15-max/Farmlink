FarmLink Photo Upload Fix — November 2026
=========================================

Issue
-----
The older Java server sent a Content-Security-Policy that blocked blob: images,
but web/js/app.js used blob: URLs to read harvest photos. The browser then
reported that the photo could not be read.

What changed
------------
- web/js/app.js now reads local files using FileReader data: URLs.
- src/main/java/com/farmlink/Main.java also permits blob: images in its CSP.
- web/post-harvest.html and web/edit-harvest.html display the updated 10 MB
  input-photo limit. Photos are automatically resized and JPEG-compressed to
  remain within the existing API/database storage limit.
- Invalid or unsupported images show a clearer error; HEIC/HEIF photos need
  exporting as JPEG or PNG.

Your database is NOT included or changed.
----------------------------------------
If installing the full project ZIP, back up your existing FarmLink folder and
copy the old FarmLink/data folder back to the replacement only if you use the
embedded HSQLDB database.
If using MySQL, your external MySQL records remain on the MySQL server.
Keep your own lib/mysql-connector-j-*.jar if you installed it manually.

To apply only the fix to an existing MySQL/HSQLDB project:
1. Stop FarmLink (Ctrl+C in the run.bat window).
2. Back up the whole FarmLink folder first.
3. Copy the following THREE files and ONE Java source from the ZIP into your
   existing FarmLink folder, preserving the same folder structure:
   web/js/app.js
   web/post-harvest.html
   web/edit-harvest.html
   src/main/java/com/farmlink/Main.java
4. Start the server with your usual run.bat command and MySQL environment
   variables, if applicable. The Java launcher recompiles the updated file.
5. Hard refresh the browser using Ctrl+F5 to load the new app.js.
6. Log in as a farmer, open Post Harvest, choose a JPEG/PNG/WebP, verify the
   preview, and publish. Open My Harvests to verify it saved correctly.

IMPORTANT: The photo is saved as compressed base64 text in the harvests.image
column. This is NOT stored as a separate image file.

If you still get an image error, test with a known-working JPEG under 10 MB,
and send the exact browser message or console error.
