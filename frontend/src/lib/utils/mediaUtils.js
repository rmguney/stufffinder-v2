import { PUBLIC_API_URL } from "$env/static/public";

export function processMediaFiles(item) {
  if (!item) return [];
  
  const mediaFiles = [];
  
  // Process media files array (for both posts and comments)
  if (item.mediaFiles && Array.isArray(item.mediaFiles) && item.mediaFiles.length > 0) {
    item.mediaFiles.forEach(media => {
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
  } 
  // Handle image URL from mystery object
  else if (item.mysteryObject?.imageUrl) {
    mediaFiles.push({
      type: 'image',
      url: item.mysteryObject.imageUrl,
      name: 'Main Image'
    });
  }
  
  return mediaFiles;
}

// Process media files for comments specifically
export function processCommentMediaFiles(comment) {
  if (!comment || !comment.mediaFiles) return [];
  
  return comment.mediaFiles.map(media => {
    const fileTypeParts = media.fileType ? media.fileType.split('/') : ['unknown'];
    const type = fileTypeParts[0] === 'image' ? 'image' : 
                 fileTypeParts[0] === 'video' ? 'video' : 
                 fileTypeParts[0] === 'audio' ? 'audio' : 'unknown';
    
    return {
      id: media.id,
      type: type,
      // FIX: Change from /api/comments/media/ to /api/mysteryObjects/media/
      url: `${PUBLIC_API_URL}/api/mysteryObjects/media/${media.id}`,
      name: media.fileName || `Media ${media.id}`,
      fileType: media.fileType
    };
  });
}
