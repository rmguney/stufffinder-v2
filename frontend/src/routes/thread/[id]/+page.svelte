<script>
  import { Textarea } from "$lib/components/ui/textarea";
  import { Button } from "$lib/components/ui/button";
  import * as Card from "$lib/components/ui/card";
  import Post from '$lib/components/post.svelte';
  import Comment from '$lib/components/comment.svelte';
  import { threadStore, updateThread, addCommentToThread, loadCommentsForThread, resolvePost, unresolvePost } from '../../../threadStore';
  import { activeUser } from '../../../userStore';
  import { onMount, onDestroy } from 'svelte';
  import { getAuthHeader } from '$lib/utils/auth';
  import { PUBLIC_API_URL } from "$env/static/public";
  import { processMediaFiles, processCommentMediaFiles } from '$lib/utils/mediaUtils';
  import { goto } from '$app/navigation';
  import { writable } from 'svelte/store';
  
  // Create a store for tag details
  const tagDetails = writable({});
  
  export let data;
  let comment = '';
  let thread;
  let isLoading = false;
  let error = null;
  let comments = [];
  let selectedCommentType = 'QUESTION';
  
  // Simple media upload functionality
  let selectedFiles = [];
  let fileInputRef;

  $: commentator = $activeUser || 'Anonymous';
  
  // This is a separate reactive statement to ensure 'comments' 
  // is updated whenever thread.comments changes
  $: {
    if (thread?.comments) {
      comments = thread.comments;
    } else {
      comments = [];
    }
  }

  // References to event listeners
  let refreshCommentListener;
  let refreshPostListener;
  
  // Resolution state
  let showResolutionModal = false;
  let resolutionDescription = '';
  let selectedCommentIds = [];
  let resolutionError = null;
  
  // Function to open resolution modal
  function openResolutionModal() {
    if ($activeUser !== thread?.author) return;
    
    showResolutionModal = true;
    resolutionDescription = thread?.resolution?.description || '';
    selectedCommentIds = thread?.resolution?.contributingComments || [];
  }
  
  // Function to handle resolution submission
  async function handleResolvePost() {
    if (!resolutionDescription.trim()) {
      resolutionError = "Please provide a resolution description";
      return;
    }
    
    try {
      resolutionError = null;
      
      const resolutionData = {
        description: resolutionDescription,
        contributingCommentIds: selectedCommentIds
      };
      
      await resolvePost(data.id, resolutionData);
      
      // Close modal and refresh
      showResolutionModal = false;
      await fetchPostWithMedia(data.id);
      
    } catch (error) {
      console.error('Error resolving post:', error);
      resolutionError = error.message || 'Failed to resolve post';
    }
  }
  
  // Function to handle unresolve
  async function handleUnresolvePost() {
    try {
      await unresolvePost(data.id);
      await fetchPostWithMedia(data.id);
    } catch (error) {
      console.error('Error unresolving post:', error);
    }
  }
  
  // Toggle comment selection for resolution
  function toggleCommentSelection(commentId) {
    if (selectedCommentIds.includes(commentId)) {
      selectedCommentIds = selectedCommentIds.filter(id => id !== commentId);
    } else {
      selectedCommentIds = [...selectedCommentIds, commentId];
    }
  }

  // Function to handle file selection
  function handleFileSelect(event) {
    const files = Array.from(event.target.files);
    if (files.length > 0) {
      selectedFiles = files;
    }
  }

  // Function to organize comments into a parent-child hierarchy
  function organizeComments(comments) {
    if (!comments || !Array.isArray(comments)) return [];
    
    // Since the API already returns comments with their replies nested,
    // we just need to ensure the structure is preserved
    const rootComments = comments.map(comment => ({
      ...comment,
      replies: comment.replies?.map(reply => ({
        ...reply,
        parentCommentId: comment.id // Ensure parent ID is set
      })) || []
    }));

    return rootComments;
  }

  async function fetchPostWithMedia(postId) {
    try {
      // Fetch post details
      const response = await fetch(`${PUBLIC_API_URL}/api/posts/getForPostDetails/${postId}`);
      if (!response.ok) throw new Error('Failed to fetch post details');
      const postData = await response.json();
      
      // Process media files into a format the frontend can use
      const mediaFiles = processMediaFiles(postData);
      
      // Check if color is a hex value and fetch color name if needed
      if (postData.mysteryObject && postData.mysteryObject.color) {
        const colorValue = postData.mysteryObject.color;
        if (isHexColor(colorValue) && !postData.mysteryObject._colorName) {
          try {
            // Store color name as a UI-only property
            const colorName = await getColorNameFromHex(colorValue);
            if (colorName) {
              postData.mysteryObject._colorName = colorName;
            }
          } catch (colorErr) {
            console.error('Error fetching color name:', colorErr);
          }
        }
      }

      // Create resolution object if post is solved
      let resolution = null;
      if (postData.solved && postData.resolutionDescription) {
        resolution = {
          description: postData.resolutionDescription,
          resolvedAt: postData.resolvedAt,
          contributingComments: postData.contributingCommentIds || []
        };
      }
      
      // Update thread store with post data and media files
      updateThread({
        id: postData.id,
        ...postData,
        mediaFiles: mediaFiles,
        mysteryObject: postData.mysteryObject ? {
          ...postData.mysteryObject,
          // Make sure we preserve any UI properties
          _colorName: postData.mysteryObject._colorName
        } : null,
        title: postData.title,
        description: postData.description,
        tags: postData.tags || [],
        author: postData.author,
        createdAt: postData.createdAt,
        updatedAt: postData.updatedAt,
        upvotes: postData.upvotes,
        downvotes: postData.downvotes,
        userUpvoted: postData.userUpvoted,
        userDownvoted: postData.userDownvoted,
        solved: postData.solved,
        resolution: resolution,
        comments: []
      });
      
      return postData;
    } catch (error) {
      console.error('Error fetching post with media:', error);
      throw error;
    }
  }

  // Helper function to check if a string is a valid hex color
  function isHexColor(str) {
    if (!str || typeof str !== 'string') return false;
    return /^#([0-9A-Fa-f]{3}){1,2}$/.test(str);
  }

  // Helper function to get color name from hex
  async function getColorNameFromHex(hexColor) {
    try {
      const cleanHex = hexColor.replace('#', '');
      const response = await fetch(`https://www.thecolorapi.com/id?hex=${cleanHex}`);
      if (!response.ok) return null;
      const data = await response.json();
      return data.name.value;
    } catch (error) {
      console.error('Error fetching color name:', error);
      return null;
    }
  }

  // Function to fetch details for tags from Wikidata
  async function fetchTagDetails(tags) {
    if (!tags || !Array.isArray(tags) || tags.length === 0) return;
    
    try {
      let fetchedTagDetails = {};
      
      for (const qcode of tags) {
        if (!qcode) continue;
        
        try {
          const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&props=labels|descriptions&origin=*`);
          const data = await response.json();
          
          if (data.entities && data.entities[qcode]) {
            const entity = data.entities[qcode];
            fetchedTagDetails[qcode] = {
              id: qcode,
              label: entity.labels?.en?.value || 'Unknown label',
              description: entity.descriptions?.en?.value || 'No description'
            };
          }
        } catch (error) {
          console.error(`Failed to fetch details for tag ${qcode}:`, error);
        }
      }
      
      tagDetails.update(existing => ({...existing, ...fetchedTagDetails}));
    } catch (error) {
      console.error("Failed to fetch tag details:", error);
    }
  }

  onMount(async () => {
    try {
      // Fetch post with media files
      await fetchPostWithMedia(data.id);
      
      // Load and organize comments
      await refreshComments();
      
      // Add event listener for comment refresh requests
      refreshCommentListener = async (event) => {
        if (event.detail?.threadId === data.id) {
          await refreshComments();
        }
      };
      
      // Add event listener for post refresh requests
      refreshPostListener = async (event) => {
        if (event.detail?.postId === data.id) {
          await fetchPostWithMedia(data.id);
        }
      };
      
      document.addEventListener('refreshComments', refreshCommentListener);
      document.addEventListener('refreshPost', refreshPostListener);
      
      // Fetch tag details if we have them
      if (thread?.tags && thread.tags.length > 0) {
        await fetchTagDetails(thread.tags);
      }
      
      // Also fetch tag details for similar posts
      const allTags = new Set(thread?.tags || []);
      similarPosts.forEach(post => {
        if (post.tags) {
          post.tags.forEach(tag => allTags.add(tag));
        }
      });
      
      if (allTags.size > 0) {
        await fetchTagDetails([...allTags]);
      }
      
    } catch (error) {
      console.error('Error fetching thread data:', error);
    }
  });
  
  onDestroy(() => {
    // Clean up the event listeners when component is destroyed
    if (refreshCommentListener) {
      document.removeEventListener('refreshComments', refreshCommentListener);
    }
    if (refreshPostListener) {
      document.removeEventListener('refreshPost', refreshPostListener);
    }
  });

  // Reactive statement to fetch tag details when thread or similarPosts change
  $: {
    if (thread?.tags && thread.tags.length > 0) {
      fetchTagDetails(thread.tags);
      
      // Also fetch tag details for similar posts
      const allTags = new Set(thread.tags);
      similarPosts.forEach(post => {
        if (post.tags) {
          post.tags.forEach(tag => allTags.add(tag));
        }
      });
      
      if (allTags.size > 0) {
        fetchTagDetails([...allTags]);
      }
    }
  }

  // Function to refresh comments
  async function refreshComments() {
    try {
      const response = await fetch(`${PUBLIC_API_URL}/api/comments/get/${data.id}`, {
        headers: {
          ...getAuthHeader()
        }
      });
      
      if (!response.ok) throw new Error('Failed to fetch comments');
      const rawComments = await response.json();
      
      // Process media files for each comment and its replies recursively
      const processCommentWithMedia = (comment) => {
        // Process media files for this comment
        const processedMediaFiles = processCommentMediaFiles(comment);
        
        // Process replies recursively
        const processedReplies = comment.replies 
          ? comment.replies.map(reply => processCommentWithMedia(reply))
          : [];
        
        // Return the comment with processed media files and replies
        return {
          ...comment,
          mediaFiles: processedMediaFiles,
          replies: processedReplies
        };
      };
      
      // Process all comments
      const processedComments = rawComments.map(comment => processCommentWithMedia(comment));
      
      // Create a hierarchical structure
      const organizedComments = organizeComments(processedComments);
      
      // Update the thread store with organized comments
      threadStore.update(threads => {
        return threads.map(t => {
          if (t.id == data.id) {
            return { ...t, comments: organizedComments };
          }
          return t;
        });
      });
    } catch (error) {
      console.error("Error refreshing comments:", error);
    }
  }

  // Enhanced comment submission with basic media upload
  let handleSend = async () => {
    if (!comment.trim() && selectedFiles.length === 0) {
      error = "Please enter a comment or attach media";
      return;
    }

    isLoading = true;
    error = null;
    
    try {
      // First, create the comment
      const payload = {
        content: comment,
        postId: data.id,
        parentCommentId: null,
        commentType: selectedCommentType,
      };
      
      const response = await fetch(`${PUBLIC_API_URL}/api/comments/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeader()
        },
        body: JSON.stringify(payload)
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to add comment: ${errorText}`);
      }
      
      const newComment = await response.json();
      const commentId = newComment.commentId;
      
      // Then, if there are files, upload each one
      if (selectedFiles.length > 0) {
        for (let i = 0; i < selectedFiles.length; i++) {
          const file = selectedFiles[i];
          const formData = new FormData();
          formData.append('file', file);
          
          try {
            // Note: This endpoint would need to be implemented on the backend
            const mediaResponse = await fetch(`${PUBLIC_API_URL}/api/comments/${commentId}/upload-media`, {
              method: 'POST',
              body: formData
            });
            
            if (!mediaResponse.ok) {
              console.error(`Failed to upload file ${i+1} for comment`);
            }
          } catch (mediaError) {
            console.error(`Error uploading file ${i+1} for comment:`, mediaError);
          }
        }
      }
      
      // Clear the input and files
      comment = '';
      selectedFiles = [];
      if (fileInputRef) fileInputRef.value = '';
      
      // Refresh comments to get updated data including media
      await refreshComments();
      
    } catch (err) {
      console.error('Error submitting comment:', err);
      error = err.message || "Failed to submit comment";
    } finally {
      isLoading = false;
    }
  };

  // Reactive statement to get the current thread from the store
  $: thread = $threadStore.find(thread => thread.id == data.id);

  // Sorting state
  let sortMethod = "newest"; // Default sort method
  let sortReplies = true; // Whether to sort replies too
  
  // New filter state
  let commentTypeFilter = "ALL"; // Default to showing all comment types
  
  // Function to sort comments based on selected criteria
  function sortCommentsRecursively(commentsToSort, method, sortChildReplies = true) {
    if (!commentsToSort || !Array.isArray(commentsToSort)) return [];
    
    // Create a copy to avoid modifying the original
    const sorted = [...commentsToSort];
    
    // Sort function based on method
    switch (method) {
      case "newest":
        sorted.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        break;
      case "oldest":
        sorted.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        break;
      case "votes":
        sorted.sort((a, b) => (b.upvotes - b.downvotes) - (a.upvotes - a.downvotes));
        break;
      case "type":
        // Order: QUESTION, SUGGESTION, STORY
        const typeOrder = { "QUESTION": 1, "SUGGESTION": 2, "STORY": 3 };
        sorted.sort((a, b) => {
          // First sort by type
          const typeComparison = (typeOrder[a.commentType] || 99) - (typeOrder[b.commentType] || 99);
          // If same type, sort by votes
          if (typeComparison === 0) {
            return (b.upvotes - b.downvotes) - (a.upvotes - a.downvotes);
          }
          return typeComparison;
        });
        break;
      default:
        // No sorting
        break;
    }
    
    // If we should sort replies too, recursively sort them
    if (sortChildReplies) {
      return sorted.map(comment => ({
        ...comment,
        replies: comment.replies && comment.replies.length > 0 
          ? sortCommentsRecursively(comment.replies, method, sortChildReplies) 
          : []
      }));
    }
    
    // Otherwise return with original reply order
    return sorted;
  }
  
  // Filter comments based on type
  function filterCommentsByType(commentsArray, typeFilter) {
    if (!commentsArray || !Array.isArray(commentsArray)) return [];
    if (typeFilter === "ALL") return commentsArray;
    
    return commentsArray.filter(comment => {
      // Keep comments that match the filter
      if (comment.commentType === typeFilter) {
        // Clone the comment to avoid modifying the original
        const filteredComment = { ...comment };
        
        // If it has replies, recursively filter them too
        if (filteredComment.replies && filteredComment.replies.length > 0) {
          filteredComment.replies = filterCommentsByType(filteredComment.replies, typeFilter);
        }
        
        return true;
      }
      
      // For non-matching parent comments, check if they have matching replies
      if (comment.replies && comment.replies.length > 0) {
        const filteredReplies = filterCommentsByType(comment.replies, typeFilter);
        
        // If any replies match, keep the parent comment but with filtered replies
        if (filteredReplies.length > 0) {
          const filteredComment = { ...comment, replies: filteredReplies };
          return true;
        }
      }
      
      // Otherwise, filter this comment out
      return false;
    });
  }
  
  // Create a sorted and filtered comments reactive variable
  $: filteredAndSortedComments = filterCommentsByType(
    sortCommentsRecursively(comments, sortMethod, sortReplies),
    commentTypeFilter
  );
  
  // Count comments by type for filter labels
  $: commentCounts = comments.reduce((acc, comment) => {
    // Count the comment
    acc[comment.commentType] = (acc[comment.commentType] || 0) + 1;
    
    // Count its replies
    if (comment.replies && comment.replies.length > 0) {
      comment.replies.forEach(reply => {
        acc[reply.commentType] = (acc[reply.commentType] || 0) + 1;
      });
    }
    
    return acc;
  }, { QUESTION: 0, SUGGESTION: 0, STORY: 0 });
  
  // Get total count for "ALL" filter
  $: totalCommentCount = (commentCounts.QUESTION || 0) + (commentCounts.SUGGESTION || 0) + (commentCounts.STORY || 0);

  // Function to find contributing comment objects by their IDs
  function findContributingComments(allComments, contributingIds) {
    if (!contributingIds || !allComments || !Array.isArray(allComments)) return [];
    
    // Create a set of contributing comment IDs for faster lookup
    const contributingIdsSet = new Set(contributingIds);
    
    // Recursive function to search through comments and their replies
    function searchComments(commentsArray) {
      let result = [];
      
      for (const comment of commentsArray) {
        if (contributingIdsSet.has(comment.id)) {
          result.push(comment);
        }
        
        // Also check replies
        if (comment.replies && comment.replies.length > 0) {
          result = [...result, ...searchComments(comment.replies)];
        }
      }
      
      return result;
    }
    
    return searchComments(allComments);
  }

  // Find contributing comments whenever comments or thread resolution changes
  $: contributingComments = thread?.resolution?.contributingComments 
    ? findContributingComments(comments, thread.resolution.contributingComments) 
    : [];

  // Find similar posts based on matching tags with the current thread
  $: similarPosts = $threadStore.filter(t => {
    // Don't include the current thread - handle string/number ID inconsistencies
    if (t.id == data.id || String(t.id) === String(data.id)) return false;
    
    // Check if there are any matching tags
    if (!thread?.tags || !t.tags) return false;
    
    // Count matching tags
    const matchingTags = t.tags.filter(tag => thread.tags.includes(tag));
    
    // Only include posts with 1 or more matching tags
    return matchingTags.length > 0;
  }).map(post => {
    // Calculate number of matching tags for sorting
    const matchingTagCount = post.tags.filter(tag => thread?.tags?.includes(tag)).length;
    return { ...post, matchingTagCount };
  }).sort((a, b) => {
    // Sort by number of matching tags (descending)
    return b.matchingTagCount - a.matchingTagCount;
  }).slice(0, 10); // Limit to 10 similar posts

  // Current slide index for carousel
  let currentSlide = 0;
  let touchStartX = 0;
  let touchEndX = 0;
  
  // Calculate visible slides based on screen width
  let visibleSlides = 3;
  let containerWidth;
  
  // Update visible slides based on container width
  $: {
    if (containerWidth) {
      if (containerWidth < 640) visibleSlides = 1;
      else if (containerWidth < 1024) visibleSlides = 2;
      else visibleSlides = 3;
    }
  }
  
  // Functions to navigate carousel
  function nextSlide() {
    if (currentSlide < similarPosts.length - visibleSlides) {
      currentSlide++;
    }
  }
  
  function prevSlide() {
    if (currentSlide > 0) {
      currentSlide--;
    }
  }
  
  // Touch handlers for mobile swipe
  function handleTouchStart(e) {
    touchStartX = e.touches[0].clientX;
  }
  
  function handleTouchEnd(e) {
    touchEndX = e.changedTouches[0].clientX;
    handleSwipe();
  }
  
  function handleSwipe() {
    const swipeDistance = touchEndX - touchStartX;
    if (swipeDistance > 50) {
      // Swipe right
      prevSlide();
    } else if (swipeDistance < -50) {
      // Swipe left
      nextSlide();
    }
  }

</script>

{#if showResolutionModal}
  <div class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4 transition-all duration-300">
    <div class="bg-white dark:dark:bg-neutral-950 rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-neutral-200 dark:border-neutral-800 animate-in fade-in zoom-in-95 duration-200">
      <div class="p-6">
        <h2 class="text-xl font-semibold mb-4 flex items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 text-teal-600 dark:text-teal-500" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
          </svg>
          Resolve Mystery Object
        </h2>
        
        {#if resolutionError}
          <div class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-3 rounded-lg mb-4 border border-red-200 dark:border-red-800/50">
            {resolutionError}
          </div>
        {/if}
        
        <div class="mb-5">
          <label for="resolution-description" class="block text-sm font-medium mb-2 text-neutral-700 dark:text-neutral-300">Resolution Description*</label>
          <Textarea 
            id="resolution-description" 
            bind:value={resolutionDescription} 
            class="w-full p-3 border rounded-lg text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500"
            placeholder="Explain how this mystery object was resolved..."
            rows="4"
          />
        </div>
        
        <div class="mb-5">
          <h3 class="text-sm font-medium mb-2 flex items-center text-neutral-700 dark:text-neutral-300">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1 text-neutral-500" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
            </svg>
            Contributing Comments
          </h3>
          <p class="text-xs text-neutral-600 dark:text-neutral-400 mb-2">
            Select comments that contributed to the resolution:
          </p>
          
          {#if comments.length > 0}
            <div class="max-h-60 overflow-y-auto border border-neutral-200 dark:border-neutral-700 rounded-lg p-2 shadow-inner">
              {#each comments as comment}
                <div class="flex items-center gap-2 p-2 hover:bg-neutral-100 dark:bg-neutral-950 rounded-md transition-colors duration-150">
                  <input 
                    type="checkbox" 
                    id={`comment-${comment.id}`}
                    checked={selectedCommentIds.includes(comment.id)}
                    on:change={() => toggleCommentSelection(comment.id)}
                    class="h-4 w-4 text-teal-600 focus:ring-teal-500 rounded-sm"
                  />
                  <label for={`comment-${comment.id}`} class="flex-1 cursor-pointer">
                    <div class="text-sm font-medium flex items-center">
                      {comment.author}
                      {#if comment.commentType}
                        <span class="ml-2 px-1.5 py-0.5 text-xs rounded-full bg-neutral-100 dark:bg-neutral-950 text-neutral-600 dark:text-neutral-400">
                          {comment.commentType.toLowerCase()}
                        </span>
                      {/if}
                    </div>
                    <div class="text-xs text-neutral-600 dark:text-neutral-400 line-clamp-1">{comment.content}</div>
                  </label>
                </div>
                
                {#if comment.replies && comment.replies.length > 0}
                  {#each comment.replies as reply}
                    <div class="flex items-center gap-2 p-2 hover:bg-neutral-100 dark:bg-neutral-950 rounded-md ml-6 border-l-2 border-neutral-200 dark:border-neutral-700 pl-4 transition-colors duration-150">
                      <input 
                        type="checkbox" 
                        id={`comment-${reply.id}`}
                        checked={selectedCommentIds.includes(reply.id)}
                        on:change={() => toggleCommentSelection(reply.id)}
                        class="h-4 w-4 text-teal-600 focus:ring-teal-500 rounded-sm"
                      />
                      <label for={`comment-${reply.id}`} class="flex-1 cursor-pointer">
                        <div class="text-sm font-medium flex items-center">
                          {reply.author} 
                          <span class="ml-1 text-xs text-neutral-500">(reply)</span>
                          {#if reply.commentType}
                            <span class="ml-2 px-1.5 py-0.5 text-xs rounded-full bg-neutral-100 dark:bg-neutral-950 text-neutral-600 dark:text-neutral-400">
                              {reply.commentType.toLowerCase()}
                            </span>
                          {/if}
                        </div>
                        <div class="text-xs text-neutral-600 dark:text-neutral-400 line-clamp-1">{reply.content}</div>
                      </label>
                    </div>
                  {/each}
                {/if}
              {/each}
            </div>
          {:else}
            <div class="text-sm text-neutral-500 dark:text-neutral-400 italic flex items-center justify-center p-4 border border-dashed border-neutral-300 dark:border-neutral-700 rounded-lg">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 opacity-70" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
              </svg>
              No comments available to select
            </div>
          {/if}
        </div>
        
        <div class="flex justify-end gap-3 mt-6">
          <Button 
            variant="outline" 
            on:click={() => showResolutionModal = false}
            class="text-sm px-4"
          >
            Cancel
          </Button>
          <Button 
            variant="default" 
            on:click={handleResolvePost}
            class="text-sm bg-teal-600 hover:bg-teal-700 px-4"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
            </svg>
            Resolve
          </Button>
        </div>
      </div>
    </div>
  </div>
{/if}

<div class="flex flex-col items-center bg-change dark:bg-dark shifting p-3 py-5">
  <div class="w-full max-w-7xl mx-auto">
    {#if thread}
      <div class="bg-white dark:dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
        <Post 
          id={data.id}
          title={thread.title}
          description={thread.description}
          tags={thread.tags}
          mysteryObject={thread.mysteryObject}
          imageSrc={thread.mysteryObjectImage ? `data:image/png;base64,${thread.mysteryObjectImage}` : ''}
          mediaFiles={thread.mediaFiles || []}
          postedBy={thread.author}
          postedDate={thread.createdAt}
          upvotes={thread.upvotes}
          downvotes={thread.downvotes}
          userUpvoted={thread.userUpvoted}
          userDownvoted={thread.userDownvoted}
          createdAt={thread.createdAt}
          updatedAt={thread.updatedAt}
          solved={thread.solved}
          resolution={thread.resolution}
          variant="thread"
          currentUser={$activeUser}
          contributingCommentObjects={contributingComments}
          on:edit={() => goto(`/edit/${thread.id}`)}
          on:resolve={openResolutionModal}
          on:unresolve={handleUnresolvePost}
        />
      </div>
    {/if}
    
    {#if thread?.tags && thread.tags.length > 0}
      {#if similarPosts.length > 0}
        <div class="mt-3 mb-1">
          <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
            <div class="p-2.5 border-b border-neutral-100 dark:border-neutral-800 flex items-center justify-between">
              <div>
                <h3 class="text-sm font-medium text-neutral-900 dark:text-white flex items-center">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5 text-teal-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                  Similar Posts
                </h3>
                <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
                  Based on {thread.tags.length} tag{thread.tags.length !== 1 ? 's' : ''}
                </p>
              </div>
              
              <!-- Navigation controls in header -->
              <div class="flex items-center gap-2">
                <button 
                  class="w-7 h-7 flex items-center justify-center rounded-full bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 disabled:opacity-40 disabled:cursor-not-allowed"
                  on:click={prevSlide}
                  disabled={currentSlide === 0}
                  aria-label="Previous slide"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
                <button 
                  class="w-7 h-7 flex items-center justify-center rounded-full bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 disabled:opacity-40 disabled:cursor-not-allowed"
                  on:click={nextSlide}
                  disabled={currentSlide >= similarPosts.length - visibleSlides}
                  aria-label="Next slide"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>
            </div>
            
            <div 
              class="relative overflow-hidden"
              bind:clientWidth={containerWidth}
            >
              <div 
                class="flex py-2.5 px-1"
                on:touchstart={handleTouchStart}
                on:touchend={handleTouchEnd}
              >
                <div 
                  class="flex transition-transform duration-300 ease-in-out"
                  style="transform: translateX(-{currentSlide * (100 / visibleSlides)}%); width: {similarPosts.length * (100 / visibleSlides)}%"
                >
                  {#each similarPosts as post}
                    <div class="px-1.5" style="width: {50 / similarPosts.length}%">
                      <a href={`/thread/${post.id}`} class="block h-full">
                        <div class="bg-neutral-50 dark:bg-neutral-900 rounded border border-neutral-200 dark:border-neutral-800 shadow-sm h-full hover:border-neutral-300 dark:hover:border-neutral-700 transition-all">
                          <!-- Compact image container -->
                          {#if post.mysteryObjectImageUrl || (post.mediaFiles && post.mediaFiles.length > 0)}
                            <div class="aspect-[3/2] overflow-hidden border-b border-neutral-200 dark:border-neutral-800 bg-neutral-100 dark:bg-neutral-800">
                              <img 
                                src={post.mysteryObjectImageUrl || (post.mediaFiles && post.mediaFiles.length > 0 ? post.mediaFiles[0].url : '')} 
                                alt={post.title} 
                                class="w-full h-full object-cover"
                              />
                            </div>
                          {:else}
                            <div class="h-3"></div>
                          {/if}
                          
                          <!-- Compact content -->
                          <div class="p-2">
                            <h4 class="font-medium text-xs line-clamp-1 mb-1">{post.title}</h4>
                            
                            <!-- Tags in single row with overflow -->
                            <div class="flex items-center gap-1 mb-1 overflow-x-auto scrollbar-none">
                              <span class="shrink-0 px-1.5 py-0.5 rounded-full text-[10px] bg-teal-100 dark:bg-teal-900/30 text-teal-800 dark:text-teal-300 border border-teal-200 dark:border-teal-800/50 flex items-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-2.5 w-2.5 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                                  <path fill-rule="evenodd" d="M17.707 9.293a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A.997.997 0 012 10V5a3 3 0 013-3h5c.256 0 .512.098.707.293l7 7zM5 6a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                                </svg>
                                <span>{post.matchingTagCount}</span>
                              </span>
                              
                              {#each post.tags.filter(tag => thread?.tags?.includes(tag)).slice(0, 1) as tag}
                                {#if $tagDetails[tag]}
                                  <span class="shrink-0 px-1.5 py-0.5 rounded-full text-[10px] bg-blue-100 dark:bg-blue-900/20 text-blue-800 dark:text-blue-300 border border-blue-200 dark:border-blue-800/30 truncate max-w-[80px]">
                                    {$tagDetails[tag].label}
                                  </span>
                                {/if}
                              {/each}
                              
                              {#if post.tags.filter(tag => thread?.tags?.includes(tag)).length > 1}
                                <span class="shrink-0 px-1 py-0.5 rounded-full text-[10px] bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300">
                                  +{post.tags.filter(tag => thread?.tags?.includes(tag)).length - 1}
                                </span>
                              {/if}
                            </div>
                            
                            <!-- Compact meta info -->
                            <div class="flex justify-between items-center text-[10px] text-neutral-500 dark:text-neutral-400 pt-1 border-t border-neutral-100 dark:border-neutral-800">
                              <div class="truncate">{post.author}</div>
                              <div class="flex items-center gap-1.5">
                                {#if post.solved}
                                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 text-emerald-600 dark:text-emerald-500" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                                  </svg>
                                {/if}
                                <div class="flex items-center">
                                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
                                  </svg>
                                  {post.commentCount || 0}
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </a>
                    </div>
                  {/each}
                </div>
              </div>
              
              <!-- Pagination dots for mobile -->
              {#if similarPosts.length > visibleSlides}
                <div class="flex justify-center pb-1.5 gap-1 sm:hidden">
                  {#each Array(Math.ceil(similarPosts.length / visibleSlides)) as _, i}
                    <button 
                      class="w-1.5 h-1.5 rounded-full transition-colors {i === Math.floor(currentSlide / visibleSlides) ? 'bg-teal-600' : 'bg-neutral-300 dark:bg-neutral-700'}"
                      on:click={() => currentSlide = i * visibleSlides}
                      aria-label={`Go to slide ${i + 1}`}
                    ></button>
                  {/each}
                </div>
              {/if}
            </div>
          </div>
        </div>
      {:else}
        <!-- No similar posts found -->
        <div class="mt-3 mb-1">
          <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden p-2.5">
            <div class="flex items-center gap-2.5">
              <div class="w-10 h-10 rounded-full bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div>
                <h3 class="text-sm font-medium text-neutral-900 dark:text-white">No similar posts found</h3>
                <p class="text-xs text-neutral-500 dark:text-neutral-400">We couldn't find any posts with matching tags</p>
              </div>
            </div>
          </div>
        </div>
      {/if}
    {:else}
      <!-- No tags in thread -->
      <div class="mt-3 mb-1">
        <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden p-2.5">
          <div class="flex items-center gap-2.5">
            <div class="w-10 h-10 rounded-full bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center shrink-0">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
              </svg>
            </div>
            <div>
              <h3 class="text-sm font-medium text-neutral-900 dark:text-white">No tags in this post</h3>
              <p class="text-xs text-neutral-500 dark:text-neutral-400">Add tags to this post to discover similar content</p>
            </div>
          </div>
        </div>
      </div>
    {/if}
    
    <!-- Grid container for comment input and filter sections, side-by-side on larger screens -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-3 mt-3 mb-3">
      <!-- Filter component with consistent styling - MOVED UP -->
      <div class="lg:col-span-5">
        <div class="flex flex-col h-full">
          <div class="w-full bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 h-full p-4 pb-3">
            <!-- Title for the discussions section -->
            <h3 class="text-md font-medium text-neutral-900 dark:text-white mb-2 -mt-1 flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-2 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              Discussions
            </h3>
            
            <!-- Comment count and filter badges with consistent styling -->
            <div class="flex flex-wrap items-center gap-2 mb-4">
              <span class="text-sm font-medium text-neutral-600 dark:text-neutral-400">Filter by:</span>
              
              <button 
                class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border transition-colors
                  {commentTypeFilter === 'ALL' 
                    ? 'bg-neutral-900 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
                    : 'bg-neutral-100 dark:bg-neutral-900 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-900'}"
                on:click={() => commentTypeFilter = 'ALL'}
              >
                <span class="flex items-center">All</span>
                <span class="ml-1.5 px-1.5 py-0.5 text-xs rounded-full bg-white/20 dark:bg-black/20 flex items-center">{totalCommentCount}</span>
              </button>
              
              <button 
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium transition-colors
                  {commentTypeFilter === 'QUESTION' 
                    ? 'bg-amber-600 text-white border-amber-700' 
                    : 'bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-800/50 hover:bg-amber-200 dark:hover:bg-amber-900/50'}"
                on:click={() => commentTypeFilter = 'QUESTION'}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                </svg>
                <span>Questions</span>
                <span class="ml-1 px-1.5 py-0.5 text-xs rounded-full bg-white/20">{commentCounts.QUESTION || 0}</span>
              </button>
              
              <button 
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium transition-colors
                  {commentTypeFilter === 'SUGGESTION' 
                    ? 'bg-emerald-600 text-white border-emerald-700' 
                    : 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/50 hover:bg-emerald-200 dark:hover:bg-emerald-900/50'}"
                on:click={() => commentTypeFilter = 'SUGGESTION'}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z" />
                </svg>
                <span>Suggestions</span>
                <span class="ml-1 px-1.5 py-0.5 text-xs rounded-full bg-white/20">{commentCounts.SUGGESTION || 0}</span>
              </button>
              
              <button 
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium transition-colors
                  {commentTypeFilter === 'STORY' 
                    ? 'bg-blue-600 text-white border-blue-700' 
                    : 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 border border-blue-200 dark:border-blue-800/50 hover:bg-blue-200 dark:hover:bg-blue-900/50'}"
                on:click={() => commentTypeFilter = 'STORY'}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M9 4.804A7.968 7.968 0 005.5 4c-1.255 0-2.443.29-3.5.804v10A7.969 7.969 0 015.5 14c1.669 0 3.218.51 4.5 1.385A7.962 7.962 0 0114.5 14c1.255 0 2.443.29 3.5.804v-10A7.968 7.968 0 0014.5 4c-1.255 0-2.443.29-3.5.804V12a1 1 0 11-2 0V4.804z" />
                </svg>
                <span>Stories</span>
                <span class="ml-1 px-1.5 py-0.5 text-xs rounded-full bg-white/20">{commentCounts.STORY || 0}</span>
              </button>
            </div>
            
            <!-- Sort controls with button styling instead of dropdown -->
            <div class="flex flex-wrap items-center gap-2 mb-2.5">
              <span class="text-sm font-medium text-neutral-600 dark:text-neutral-400">Sort by:</span>
              
              <button 
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border transition-colors
                  {sortMethod === 'newest' 
                    ? 'bg-neutral-900 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
                    : 'bg-neutral-100 dark:bg-neutral-900 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-900'}"
                on:click={() => sortMethod = 'newest'}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd" />
                </svg>
                Newest
              </button>
              
              <button 
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border transition-colors
                  {sortMethod === 'oldest' 
                    ? 'bg-neutral-900 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
                    : 'bg-neutral-100 dark:bg-neutral-900 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-900'}"
                on:click={() => sortMethod = 'oldest'}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-14a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 7.586V4z" clip-rule="evenodd" />
                </svg>
                Oldest
              </button>
              
              <button 
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border transition-colors
                  {sortMethod === 'votes' 
                    ? 'bg-neutral-900 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
                    : 'bg-neutral-100 dark:bg-neutral-900 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-900'}"
                on:click={() => sortMethod = 'votes'}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M2 10.5a1.5 1.5 0 113 0v6a1.5 1.5 0 01-3 0v-6zM6 10.333v5.43a2 2 0 001.106 1.79l.05.025A4 4 0 008.943 18h5.416a2 2 0 001.962-1.608l1.2-6A2 2 0 0015.56 8H12V4a2 2 0 00-2-2 1 1 0 00-1 1v.667a4 4 0 01-.8 2.4L6.8 7.933a4 4 0 00-.8 2.4z" />
                </svg>
                Most votes
              </button>
              
              <button 
                class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border transition-colors
                  {sortMethod === 'type' 
                    ? 'bg-neutral-900 dark:bg-neutral-200 text-white dark:text-neutral-900 border-neutral-700 dark:border-neutral-300' 
                    : 'bg-neutral-100 dark:bg-neutral-900 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-900'}"
                on:click={() => sortMethod = 'type'}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                </svg>
                By type
              </button>
            </div>
            
            <div class="flex items-center mb-2">
              <label class="inline-flex items-center text-sm text-neutral-500 dark:text-neutral-400">
                <input 
                  type="checkbox" 
                  bind:checked={sortReplies} 
                  class="h-3 w-3 text-teal-600 focus:ring-teal-500 mr-1.5 rounded-full"
                />
                Also sort replies
              </label>
            </div>
            
            <!-- Results info with consistent styling -->
            <div class="pt-2 text-neutral-500 dark:text-neutral-400 border-t border-neutral-100 dark:border-neutral-800 text-xs">
              {#if commentTypeFilter === 'ALL'}
                Showing all comments
              {:else if commentTypeFilter !== 'ALL' && filteredAndSortedComments.length !== comments.length}
                Showing {filteredAndSortedComments.length} of {comments.length} comments
                <button 
                  class="ml-2 text-teal-600 dark:text-teal-500 hover:underline"
                  on:click={() => commentTypeFilter = 'ALL'}
                >
                  Clear filter
                </button>
              {/if}
            </div>
          </div>
        </div>
      </div>

      <!-- Comment input area with consistent styling - MOVED DOWN -->
      <div class="lg:col-span-7">
        {#if $activeUser}
          <Card.Root class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden h-full">
            <div class="w-full p-4">
              <!-- Consistent padding -->
              <Textarea 
                bind:value={comment} 
                class="w-full resize-none p-3 border border-neutral-200 dark:border-neutral-700 rounded-md bg-white dark:dark:bg-neutral-950 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 mb-3" 
                placeholder="Write your comment..."
              />
              
              <div class="w-full flex flex-wrap items-start gap-3">
                <div class="flex flex-wrap items-center gap-3">
                  <div class="flex items-center bg-neutral-100 dark:bg-neutral-950 rounded-full p-1 border border-neutral-200 dark:border-neutral-700">
                    <label class="inline-flex items-center px-2.5 py-1 rounded-full cursor-pointer transition-all text-xs
                                  {selectedCommentType === 'QUESTION' ? 'bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-300 shadow-sm' : 'text-neutral-700 dark:text-neutral-300 hover:bg-neutral-200 dark:hover:bg-neutral-900'}">
                      <input type="radio" id="question" bind:group={selectedCommentType} value="QUESTION" class="sr-only" />
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                      </svg>
                      Question
                    </label>
                    
                    <label class="inline-flex items-center px-2.5 py-1 rounded-full cursor-pointer transition-all text-xs
                                  {selectedCommentType === 'SUGGESTION' ? 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-300 shadow-sm' : 'text-neutral-700 dark:text-neutral-300 hover:bg-neutral-200 dark:hover:bg-neutral-900'}">
                      <input type="radio" id="suggestion" bind:group={selectedCommentType} value="SUGGESTION" class="sr-only" />
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z" />
                      </svg>
                      Suggestion
                    </label>
                    
                    <label class="inline-flex items-center px-2.5 py-1 rounded-full cursor-pointer transition-all text-xs
                                  {selectedCommentType === 'STORY' ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 shadow-sm' : 'text-neutral-700 dark:text-neutral-300 hover:bg-neutral-200 dark:hover:bg-neutral-900'}">
                      <input type="radio" id="story" bind:group={selectedCommentType} value="STORY" class="sr-only" />
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M9 4.804A7.968 7.968 0 005.5 4c-1.255 0-2.443.29-3.5.804v10A7.969 7.969 0 015.5 14c1.669 0 3.218.51 4.5 1.385A7.962 7.962 0 0114.5 14c1.255 0 2.443.29 3.5.804v-10A7.968 7.968 0 0014.5 4c-1.255 0-2.443.29-3.5.804V12a1 1 0 11-2 0V4.804z" />
                      </svg>
                      Story
                    </label>
                  </div>
                  
                  <!-- Media upload button -->
                  <div class="flex items-center">
                    <input 
                      type="file" 
                      id="media-upload"
                      bind:this={fileInputRef}
                      on:change={handleFileSelect}
                      accept="image/*,video/*,audio/*" 
                      class="hidden" 
                      multiple
                    />
                    <Button 
                      variant="outline" 
                      size="sm" 
                      class="text-xs bg-white dark:dark:bg-neutral-950 hover:bg-neutral-100 dark:bg-neutral-950 hover:dark:bg-neutral-900 border-neutral-300 dark:border-neutral-700 rounded-full"
                      on:click={() => fileInputRef?.click()}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
                      </svg>
                      {selectedFiles.length > 0 ? `${selectedFiles.length !== 1 ? 's' : ''}` : 'Add Media'}
                    </Button>
                  </div>
                  
                  <!-- Submit button -->
                  <Button 
                    on:click={handleSend} 
                    variant="default"
                    size="sm"
                    class="text-xs bg-teal-600 hover:bg-teal-700 text-white px-4 rounded-full"
                    disabled={isLoading}
                  >
                    {#if isLoading}
                      <span class="inline-block h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-1.5"></span>
                      {selectedFiles.length > 0 ? 'Uploading...' : 'Posting...'}
                    {:else}
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z" />
                      </svg>
                      Post as {selectedCommentType.charAt(0) + selectedCommentType.slice(1).toLowerCase()}
                    {/if}
                  </Button>
                </div>
              </div>
              
              {#if selectedFiles.length > 0}
                <div class="mt-3 flex flex-wrap gap-2 p-2 border border-neutral-200 dark:border-neutral-700 rounded-md bg-white dark:dark:bg-neutral-950">
                  {#each selectedFiles as file, i}
                    <div class="relative bg-white dark:dark:bg-neutral-950 rounded-md shadow-sm p-1 w-16 h-16 flex items-center justify-center border border-neutral-200 dark:border-neutral-700">
                      {#if file.type.startsWith('image/')}
                        <img 
                          src={URL.createObjectURL(file)} 
                          alt={file.name} 
                          class="max-w-full max-h-full object-contain rounded"
                        />
                      {:else if file.type.startsWith('video/')}
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                      {:else if file.type.startsWith('audio/')}
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2zM9 10l12-3" />
                        </svg>
                      {:else}
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                      {/if}
                      <!-- Remove button -->
                      <button 
                        class="absolute -top-1.5 -right-1.5 bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs shadow-sm hover:bg-red-600 transition-colors"
                        on:click={() => {
                          selectedFiles = selectedFiles.filter((_, index) => index !== i);
                        }}
                      >
                        ×
                      </button>
                    </div>
                  {/each}
                </div>
              {/if}
              
              {#if error}
                <div class="mt-2 text-red-500 text-sm flex items-center">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                  </svg>
                  {error}
                </div>
              {/if}
              
              <!-- Description for comment type selection -->
              <div class="mt-3 text-xs text-neutral-500 dark:text-neutral-400 border-t border-neutral-100 dark:border-neutral-800 pt-2">
                <div class="flex items-center gap-1">
                  <span class="font-medium">Posting as:</span>
                  {#if selectedCommentType === 'QUESTION'}
                    <span class="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-300">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                      </svg>
                      Question
                    </span>
                    - Ask for more details or information about the mystery object
                  {:else if selectedCommentType === 'SUGGESTION'}
                    <span class="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-300">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z" />
                      </svg>
                      Suggestion
                    </span>
                    - Offer an idea about what the mystery object might be
                  {:else}
                    <span class="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M9 4.804A7.968 7.968 0 005.5 4c-1.255 0-2.443.29-3.5.804v10A7.969 7.969 0 015.5 14c1.669 0 3.218.51 4.5 1.385A7.962 7.962 0 0114.5 14c1.255 0 2.443.29 3.5.804v-10A7.968 7.968 0 0014.5 4c-1.255 0-2.443.29-3.5.804V12a1 1 0 11-2 0V4.804z" />
                      </svg>
                      Story
                    </span>
                    - Share a personal experience or related story
                  {/if}
                </div>
              </div>
            </div>
          </Card.Root>
        {:else}
          <Card.Root class="bg-white dark:dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden h-full">
            <div class="py-6 text-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-neutral-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
              </svg>
              <Card.Title class="text-lg mb-1">Sign in to comment</Card.Title>
              <p class="text-sm text-neutral-500 dark:text-neutral-400">Join the discussion by signing in to your account</p>
            </div>
          </Card.Root>
        {/if}
      </div>
    </div>
    
    {#if comments.length > 0}
      <div class="space-y-2">
        {#each filteredAndSortedComments as commentItem (commentItem.id)}
          <div>
            <Comment
              threadId={data.id}
              commentId={commentItem.id}
              comment={commentItem.content} 
              commentator={commentItem.author}
              postedDateComment={commentItem.createdAt}
              contributingToResolution={thread?.resolution?.contributingComments?.includes(commentItem.id) || false}
              threadOwner={thread?.author}
              replies={commentItem.replies || []}
              upvotes={commentItem.upvotes}
              downvotes={commentItem.downvotes}
              userUpvoted={commentItem.userUpvoted}
              userDownvoted={commentItem.userDownvoted}
              mediaFiles={commentItem.mediaFiles || []}
              commentType={commentItem.commentType}
            />
          </div>
        {/each}
      </div>
    {:else}
      <Card.Root class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
        <div class="py-10 text-center px-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-neutral-300 mx-auto mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <Card.Title class="text-xl mb-2">There are no comments yet</Card.Title>
          <Card.Description class="max-w-md mx-auto text-base">
            Be the first to contribute to this discussion and share your thoughts!
          </Card.Description>
        </div>
      </Card.Root>
    {/if}
    
    <!-- Empty state when filter returns no results -->
    {#if comments.length > 0 && filteredAndSortedComments.length === 0}
      <Card.Root class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
        <div class="py-8 text-center px-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-neutral-300 mx-auto mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
          </svg>
          <Card.Title class="text-xl mb-2 text-left">No matching comments found</Card.Title>
          <Card.Description class="max-w-md mx-auto text-base text-left">
            No {commentTypeFilter.toLowerCase()} comments were found. Try changing your filter or checking back later.
          </Card.Description>
          <Button 
            variant="outline" 
            class="mt-4"
            on:click={() => commentTypeFilter = 'ALL'}
          >
            Show all comments
          </Button>
        </div>
      </Card.Root>
    {/if}
  </div>
</div>
