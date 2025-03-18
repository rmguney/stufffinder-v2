import { PUBLIC_API_URL } from "$env/static/public";

export function processMediaFiles(post) {
  if (!post) return [];
  
  const mediaFiles = [];
  
  if (post.mediaFiles && Array.isArray(post.mediaFiles) && post.mediaFiles.length > 0) {
    post.mediaFiles.forEach(media => {
      const fileTypeParts = media.fileType ? media.fileType.split('/') : ['unknown'];
      const type = fileTypeParts[0] === 'image' ? 'image' : 
                   fileTypeParts[0] === 'video' ? 'video' : 
                   fileTypeParts[0] === 'audio' ? 'audio' : 'unknown';
      
      mediaFiles.push({
        id: media.id,
        type: type,
        url: `${PUBLIC_API_URL}/api/mysteryObjects/media/${media.id}`,
        name: media.fileName || `Media ${media.id}`,
        fileType: media.fileType
      });
    });
  } else {
    if (post.mysteryObject?.image) {
      mediaFiles.push({
        type: 'image',
        url: `data:image/png;base64,${post.mysteryObject.image}`,
        name: 'Main Image'
      });
    } else if (post.mysteryObject?.imageUrl) {
      mediaFiles.push({
        type: 'image',
        url: post.mysteryObject.imageUrl,
        name: 'Main Image'
      });
    }
  }
  
  return mediaFiles;
}