package com.leapsy.player4b.adatper

interface ISelectFileListener {
    /**
     * open folder
     * @param aPosition
     * @param aBucketName
     * @param aNumberOfFilesInFolder
     */
    fun onFolderSelected(aPosition : Int, aBucketName : String, aNumberOfFilesInFolder : Int)

    /**
     * open file to watch or listener
     */
    fun onFileItemClicked(aPosition : Int, aMediaFileName : String)
}