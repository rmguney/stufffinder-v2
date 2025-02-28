/**
 * Utility functions for working with comment data
 */
import { PUBLIC_API_URL } from "$env/static/public";

/**
 * Organize flat comment list into a hierarchical structure
 * @param {Array} comments - Flat array of comments
 * @returns {Array} - Hierarchical array with parent-child relationships
 */
export function organizeComments(comments) {
  if (!comments || !Array.isArray(comments)) return [];
  
  const commentMap = {};
  const rootComments = [];
  
  // First, create a map of all comments by their ID
  comments.forEach(comment => {
    commentMap[comment.id] = {
      ...comment,
      replies: []
    };
  });
  
  // Now connect child comments to their parents
  comments.forEach(comment => {
    if (comment.parentCommentId) {
      // This is a child comment
      const parentComment = commentMap[comment.parentCommentId];
      if (parentComment) {
        parentComment.replies.push(commentMap[comment.id]);
      } else {
        // If parent not found, treat as root
        rootComments.push(commentMap[comment.id]);
      }
    } else {
      // This is a root comment
      rootComments.push(commentMap[comment.id]);
    }
  });
  
  return rootComments;
}

/**
 * Debug helper to print comment hierarchy
 * @param {Array} comments - Array of comments with replies
 * @param {number} depth - Current depth level (for indentation)
 */
export function printCommentHierarchy(comments, depth = 0) {
  if (!comments) return;
  
  comments.forEach(comment => {
    /* console.log(
      `${' '.repeat(depth * 2)}Comment ID: ${comment.id}, Content: ${comment.content}, Author: ${comment.author}`
    ); */
    
    if (comment.replies && comment.replies.length > 0) {
      // console.log(`${' '.repeat(depth * 2)}Replies:`);
      printCommentHierarchy(comment.replies, depth + 1);
    }
  });
}

// Test function to visualize comment structure in console
export function debugCommentStructure(threadId) {
  fetch(`${PUBLIC_API_URL}/api/comments/get/${threadId}`)
    .then(response => response.json())
    .then(comments => {
      // console.log('Raw comments:', comments);
      const organized = organizeComments(comments);
      // console.log('Organized comments:');
      printCommentHierarchy(organized);
      return organized;
    })
    .catch(error => {
      console.error('Error debugging comments:', error);
    });
}
