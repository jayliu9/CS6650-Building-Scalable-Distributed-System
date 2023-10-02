package swagger

type AlbumDAO struct {
    testObject AlbumInfo
}

func NewAlbumDAO() *AlbumDAO {
    return &AlbumDAO{
        testObject: AlbumInfo{
            Artist: "Artist",
            Title:  "Title",
            Year:   "1988", 
        },
    }
}

func (dao *AlbumDAO) GetAlbumByKey(albumId string) AlbumInfo {
    return dao.testObject
}