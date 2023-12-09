package mapper;

import dto.AlbumInfo;
import dto.AlbumProfile;
import model.Album;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper
public interface AlbumMapper {
    AlbumMapper INSTANCE = Mappers.getMapper(AlbumMapper.class);

    AlbumInfo albumToAlbumInfo(Album album);
    Album albumProfileToAlbum(AlbumProfile albumProfile);
}
