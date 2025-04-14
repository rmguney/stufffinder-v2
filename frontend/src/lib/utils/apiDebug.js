import { PUBLIC_API_URL } from "$env/static/public";

const debugCommentAPI = {
  // Test getting comments for a post
  async getComments(postId) {
    try {
      // console.log(`Fetching comments for post ${postId}...`);
      const response = await fetch(`https://backend-310608491068.europe-west1.run.app/api/comments/get/${postId}`);
      
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      
      const data = await response.json();
      // console.log('Comments received:', data);
      
      // Verify comment structure
      if (data.length > 0) {
        // console.log('First comment structure:', Object.keys(data[0]));
        // console.log('Sample comment:', data[0]);
      }
      
      return data;
    } catch (error) {
      console.error('Error getting comments:', error);
      return null;
    }
  },
  
  // Test creating a new comment
  async createComment(postId, content, parentCommentId = null) {
    try {
      // console.log(`Creating comment for post ${postId}...`);
      const payload = {
        content,
        postId,
        parentCommentId
      };
      
      // console.log('Payload:', payload);
      
      const response = await fetch(`https://backend-310608491068.europe-west1.run.app/api/comments/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
      
      const responseText = await response.text();
      // console.log(`Response status: ${response.status}, text:`, responseText);
      
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}, Response: ${responseText}`);
      }
      
      // Try to parse if it's JSON
      try {
        const data = JSON.parse(responseText);
        // console.log('Comment created:', data);
        return data;
      } catch (e) {
        // console.log('Response is not JSON:', responseText);
        return responseText;
      }
    } catch (error) {
      console.error('Error creating comment:', error);
      return null;
    }
  }
};

// Usage in console:
// debugCommentAPI.getComments(1)
// debugCommentAPI.createComment(1, "Test comment")
