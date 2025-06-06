package com.example.timecapsule.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    private String FileName; // unique stored filename
    private String originalName;   // original uploaded filename
    private String ContentType;    // MIME type like image/png, video/mp4
    private long size;             // file size in bytes
    private String storagePath;    // path where file is stored (optional)



}
