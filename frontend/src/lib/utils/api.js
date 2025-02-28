/**
 * Utility functions for making authenticated API requests
 */
import { PUBLIC_API_URL } from "$env/static/public";

const API_BASE_URL = `${PUBLIC_API_URL}`;

/**
 * Make an authenticated API request
 */
export async function apiRequest(endpoint, options = {}) {
  try {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
    
    // Get the authentication token from storage
    const token = localStorage.getItem('authToken');
    
    // Set up headers with authentication
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...(options.headers || {})
    };
    
    const response = await fetch(url, {
      ...options,
      headers
    });
    
    // Get response as text first for better error handling
    const responseText = await response.text();
    
    // Log response details for debugging
    console.log(`API ${options.method || 'GET'} ${endpoint} response:`, {
      status: response.status,
      ok: response.ok,
      responseText: responseText.substring(0, 100) + (responseText.length > 100 ? '...' : '')
    });
    
    if (!response.ok) {
      throw new Error(`API request failed: ${response.status} - ${responseText}`);
    }
    
    // Try to parse as JSON if the response is not empty
    if (responseText) {
      try {
        return JSON.parse(responseText);
      } catch (e) {
        // If parsing fails, return the raw text
        return responseText;
      }
    }
    
    return null;
  } catch (error) {
    console.error(`API request failed for ${endpoint}:`, error);
    throw error;
  }
}

/**
 * Get resource from API
 */
export function get(endpoint) {
  return apiRequest(endpoint, { method: 'GET' });
}

/**
 * Post data to API
 */
export function post(endpoint, data) {
  return apiRequest(endpoint, { 
    method: 'POST',
    body: JSON.stringify(data)
  });
}

/**
 * Put data to API
 */
export function put(endpoint, data) {
  return apiRequest(endpoint, {
    method: 'PUT',
    body: JSON.stringify(data)
  });
}

/**
 * Delete resource from API
 */
export function del(endpoint) {
  return apiRequest(endpoint, { method: 'DELETE' });
}

/**
 * Utility function to organize comment hierarchy
 */
export function organizeComments(comments) {
  if (!comments || !Array.isArray(comments)) return [];
  
  // Log input for debugging
  console.log("API - Organizing comments:", comments.length);
  
  const commentMap = {};
  const rootComments = [];
  
  // Create a map of all comments by ID
  comments.forEach(comment => {
    commentMap[comment.id] = {
      ...comment,
      replies: []
    };
  });
  
  // Build the hierarchy
  comments.forEach(comment => {
    // Check if we have parentId field
    const parentId = comment.parentCommentId || comment.parentId;
    
    if (parentId && commentMap[parentId]) {
      commentMap[parentId].replies.push(commentMap[comment.id]);
    } else {
      rootComments.push(commentMap[comment.id]);
    }
  });
  
  return rootComments;
}
