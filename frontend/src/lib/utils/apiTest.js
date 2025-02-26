// You can run this in the browser console to test the comment API directly

async function testCommentAPI() {
  try {
    const response = await fetch('http://localhost:8080/api/comments/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': localStorage.getItem('authToken') || ''
      },
      body: JSON.stringify({
        content: "Test comment from API test",
        postId: 1, // Replace with an actual post ID
        parentCommentId: null
      })
    });
    
    const data = await response.json();
    console.log('API test result:', data);
  } catch (error) {
    console.error('API test error:', error);
  }
}

// Run with: testCommentAPI()
