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
</script>

<!-- Resolution Modal -->
{#if showResolutionModal}
  <div class="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
    <div class="bg-white dark:bg-neutral-900 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
      <div class="p-6">
        <h2 class="text-xl font-semibold mb-4">Resolve Mystery Object</h2>
        
        {#if resolutionError}
          <div class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-3 rounded-md mb-4">
            {resolutionError}
          </div>
        {/if}
        
        <div class="mb-4">
          <label for="resolution-description" class="block text-sm font-medium mb-1.5">Resolution Description*</label>
          <Textarea 
            id="resolution-description" 
            bind:value={resolutionDescription} 
            class="w-full p-2 border rounded-lg text-sm bg-white dark:bg-neutral-800 border-neutral-200 dark:border-neutral-700"
            placeholder="Explain how this mystery object was resolved..."
            rows="4"
          />
        </div>
        
        <div class="mb-4">
          <h3 class="text-sm font-medium mb-2">Contributing Comments</h3>
          <p class="text-xs text-neutral-600 dark:text-neutral-400 mb-2">
            Select comments that contributed to the resolution:
          </p>
          
          {#if comments.length > 0}
            <div class="max-h-60 overflow-y-auto border border-neutral-200 dark:border-neutral-700 rounded-lg p-2">
              {#each comments as comment}
                <div class="flex items-center gap-2 p-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-md">
                  <input 
                    type="checkbox" 
                    id={`comment-${comment.id}`}
                    checked={selectedCommentIds.includes(comment.id)}
                    on:change={() => toggleCommentSelection(comment.id)}
                    class="h-4 w-4 text-teal-600 focus:ring-teal-500 border-neutral-300 rounded"
                  />
                  <label for={`comment-${comment.id}`} class="flex-1 cursor-pointer">
                    <div class="text-sm font-medium">{comment.author}</div>
                    <div class="text-xs text-neutral-600 dark:text-neutral-400 line-clamp-1">{comment.content}</div>
                  </label>
                </div>
                
                {#if comment.replies && comment.replies.length > 0}
                  {#each comment.replies as reply}
                    <div class="flex items-center gap-2 p-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-md ml-6 border-l-2 border-neutral-200 dark:border-neutral-700 pl-4">
                      <input 
                        type="checkbox" 
                        id={`comment-${reply.id}`}
                        checked={selectedCommentIds.includes(reply.id)}
                        on:change={() => toggleCommentSelection(reply.id)}
                        class="h-4 w-4 text-teal-600 focus:ring-teal-500 border-neutral-300 rounded"
                      />
                      <label for={`comment-${reply.id}`} class="flex-1 cursor-pointer">
                        <div class="text-sm font-medium">{reply.author} <span class="text-xs text-neutral-500">(reply)</span></div>
                        <div class="text-xs text-neutral-600 dark:text-neutral-400 line-clamp-1">{reply.content}</div>
                      </label>
                    </div>
                  {/each}
                {/if}
              {/each}
            </div>
          {:else}
            <div class="text-sm text-neutral-500 dark:text-neutral-400 italic">
              No comments available to select
            </div>
          {/if}
        </div>
        
        <div class="flex justify-end gap-2 mt-6">
          <Button 
            variant="outline" 
            on:click={() => showResolutionModal = false}
            class="text-sm"
          >
            Cancel
          </Button>
          <Button 
            variant="default" 
            on:click={handleResolvePost}
            class="text-sm bg-teal-600 hover:bg-teal-700"
          >
            Resolve
          </Button>
        </div>
      </div>
    </div>
  </div>
{/if}

<div class="flex flex-col items-center bg-change dark:bg-dark shifting p-4 lg:p-8">
  <div class="w-full lg:w-2/3">
    {#if thread}
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
      />
    {/if}

    {#if thread && $activeUser === thread.author}
      <div class="mt-4 flex justify-end gap-2">
        <Button 
          variant="outline"
          class="text-xs py-1 px-3 hover:bg-rose-900 hover:text-white transition-colors"
          on:click={() => goto(`/edit/${thread.id}`)}
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
            <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
          </svg>
          Edit Post
        </Button>
        
        {#if !thread.solved}
          <Button 
            variant="outline"
            class="text-xs py-1 px-3 border-teal-300 dark:border-teal-700 text-teal-700 dark:text-teal-400 hover:bg-teal-50 dark:hover:bg-teal-900/20"
            on:click={openResolutionModal}
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
            </svg>
            Mark as Resolved
          </Button>
        {:else}
          <Button 
            variant="outline"
            class="text-xs py-1 px-3 border-rose-300 dark:border-rose-700 text-rose-700 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-900/20"
            on:click={handleUnresolvePost}
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
            </svg>
            Revert Resolution
          </Button>
        {/if}
      </div>
    {/if}
    
    {#if $activeUser}
    <Card.Root class="bg-opacity-90 hover:bg-opacity-100 p-4 mt-4 flex flex-col justify-center items-center">
      <div class="w-full space-y-4">
        <div class="flex justify-center gap-4 mb-2">
          <div class="flex items-center space-x-2">
            <input type="radio" id="question" bind:group={selectedCommentType} value="QUESTION" class="mr-1" />
            <label for="question" class="text-sm">Question</label>
          </div>
          <div class="flex items-center space-x-2">
            <input type="radio" id="suggestion" bind:group={selectedCommentType} value="SUGGESTION" class="mr-1" />
            <label for="suggestion" class="text-sm">Suggestion</label>
          </div>
          <div class="flex items-center space-x-2">
            <input type="radio" id="story" bind:group={selectedCommentType} value="STORY" class="mr-1" />
            <label for="story" class="text-sm">Story</label>
          </div>
        </div>

        <Textarea 
          bind:value={comment} 
          class="w-full resize-none p-2" 
          placeholder="Write your comment..."
        />
        
        <!-- Simplified file input for media -->
        <div class="w-full">
          <div class="flex items-center justify-center gap-2">
            <div class="flex items-center space-x-2">
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
                class="text-xs hover:bg-neutral-100 dark:hover:bg-neutral-800"
                on:click={() => fileInputRef?.click()}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
                </svg>
                Add Media
              </Button>
              {#if selectedFiles.length > 0}
                <span class="text-xs text-neutral-500">
                  {selectedFiles.length} file{selectedFiles.length !== 1 ? 's' : ''} selected
                </span>
              {/if}
            </div>
            
            <!-- Post Comment button moved here -->
            <Button 
              on:click={handleSend} 
              variant="outline"
              size="sm"
              class="text-xs hover:bg-neutral-100 dark:hover:bg-neutral-800"
              disabled={isLoading}
            >
              {#if isLoading}
                <span class="inline-block h-4 w-4 border-2 border-current/30 border-t-current rounded-full animate-spin mr-2"></span>
                {selectedFiles.length > 0 ? 'Uploading...' : 'Sending...'}
              {:else}
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z" />
                </svg>
                Post Comment
              {/if}
            </Button>
          </div>
          
          <!-- Simple file preview -->
          {#if selectedFiles.length > 0}
            <div class="mt-2 flex flex-wrap gap-2">
              {#each selectedFiles as file, i}
                <div class="relative bg-neutral-100 dark:bg-neutral-800 rounded p-1 w-16 h-16 flex items-center justify-center">
                  {#if file.type.startsWith('image/')}
                    <img 
                      src={URL.createObjectURL(file)} 
                      alt={file.name} 
                      class="max-w-full max-h-full object-contain"
                    />
                  {:else if file.type.startsWith('video/')}
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  {:else if file.type.startsWith('audio/')}
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                    </svg>
                  {:else}
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  {/if}
                  <!-- Remove button -->
                  <button 
                    class="absolute -top-1 -right-1 bg-red-500 text-white rounded-full w-4 h-4 flex items-center justify-center text-xs"
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
        </div>
        
        {#if error}
          <p class="text-red-500 text-sm">{error}</p>
        {/if}
      </div>
    </Card.Root>
    {:else}
    <Card.Root class="bg-opacity-90 hover:bg-opacity-100 mt-4">
      <Card.Header>
        <Card.Title class="text-lg text-center pb-6">Sign in to comment</Card.Title>
      </Card.Header>
    </Card.Root>
    {/if}
    <div class="flex flex-col justify-center pt-2">
      <div class="flex items-center mb-1">
        <div class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-neutral-50 dark:bg-neutral-950 text-neutral-800 dark:text-neutral-200 border border-neutral-200 dark:border-neutral-700">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
          </svg>
          Comments: {comments.length}
        </div>
      </div>
      {#if comments.length > 0}
        {#each comments as commentItem (commentItem.id)}
          <div class="mb-2">
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
      {:else}
        <Card.Root class="bg-opacity-90 hover:bg-opacity-100">
          <Card.Header>
            <Card.Title class="pb-2 text-lg text-center">There are no comments yet</Card.Title>
          </Card.Header>
          <Card.Description class="p-4 text-center">
            Be the first to comment!
          </Card.Description>
        </Card.Root>
      {/if}
    </div>
  </div>
</div>
