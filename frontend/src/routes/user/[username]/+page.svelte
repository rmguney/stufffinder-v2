<script>
  import * as Card from '$lib/components/ui/card/index.js';
  import { Button } from '$lib/components/ui/button';
  import Post from '$lib/components/post.svelte';
  import Comment from '$lib/components/comment.svelte';
  import { threadStore } from '../../../threadStore';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { PUBLIC_API_URL } from "$env/static/public";
  import { fetchTagLabels, tagMap } from "$lib/utils/fetchTags.js";

  // State variables
  let userId;
  let userName;
  let threads = [];
  let comments = [];
  let loadingThreads = true;
  let loadingComments = true;
  let tagsLoaded = false;
  
  // Get username from URL params
  $: userName = $page.params.username;
  
  // Format date in the same style as the Angular implementation
  function formatDate(date) {
    if (!date) return 'Unknown date';
    try {
      return new Date(date).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch (error) {
      console.error("Error formatting date:", error);
      return 'Unknown date';
    }
  }
  
  // Process thread data to ensure all expected fields exist
  function processThreadData(thread) {
        return {
            ...thread,
            title: thread.title || "Untitled Post",
            description: thread.description || thread.content || "",
            createdAt: thread.createdAt || thread.created || thread.date || null,
            tags: Array.isArray(thread.tags) ? thread.tags : 
                  (thread.categories ? thread.categories : []),
            resolvedTags: thread.tags?.map(qcode => tagMap.get(qcode) || "Loading...") || [] // Q kodlarını geçici olarak göster
        };
  }

  // Fetches all q codes and updates tags inside the thread
  async function updateTags() {
        const allQcodes = threads.flatMap(thread => thread.tags).filter(qcode => qcode);

        if (allQcodes.length > 0) {
            await fetchTagLabels(allQcodes);
            tagsLoaded = true;

            // Update tags inside threads
            threads = threads.map(thread => ({
                ...thread,
                resolvedTags: thread.tags.map(qcode => tagMap.get(qcode) || qcode)
            }));
        }
  }
  
  // Fetch user data, threads and comments
  onMount(async () => {
    try {
      loadingThreads = true;
      loadingComments = true;
      
      // First get the userId from the username
      const userResponse = await fetch(`${PUBLIC_API_URL}/api/auth/username/${userName}`);
      if (!userResponse.ok) throw new Error('User not found');
      const userData = await userResponse.json();
      userId = userData.userId;
      
      // Fetch user's posts (threads)
      const postsResponse = await fetch(`${PUBLIC_API_URL}/api/auth/${userId}/posts`);
      if (!postsResponse.ok) throw new Error('Failed to fetch posts');
      const postsData = await postsResponse.json();

      console.log("User's posts data:", postsData);
      
      // Process each thread to ensure consistent data structure
      const processedThreads = postsData.map(processThreadData);
      
      // Sort posts by most recent first
      threads = processedThreads.sort((a, b) => {
        const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return dateB - dateA;
      });
      
      // Update the thread store
      threadStore.set(threads);

      await updateTags();
      
      // Fetch user's comments
      const commentsResponse = await fetch(`${PUBLIC_API_URL}/api/auth/${userId}/comments`);
      if (!commentsResponse.ok) throw new Error('Failed to fetch comments');
      const commentsData = await commentsResponse.json();
      
      // Sort comments by most recent first
      comments = commentsData.sort((a, b) => 
        new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
      );
      
    } catch (error) {
      console.error("Error fetching data:", error);
    } finally {
      loadingThreads = false;
      loadingComments = false;
    }
  });
</script>

<div class="min-h-screen flex justify-center p-4 md:p-6 lg:py-10 bg-change dark:bg-dark shifting">
  <div class="w-full max-w-3xl">
    <!-- Profile Header -->
    <Card.Root class="bg-opacity-90 mb-6 transition-transform hover:scale-[1.01] duration-200">
      <Card.Header class="flex items-center p-6">
        <div class="flex flex-col sm:flex-row items-center gap-4">
          <div class="flex items-center justify-center w-20 h-20 rounded-full bg-neutral-100 dark:bg-neutral-900">
            <span class="text-2xl font-medium">
              {userName ? userName[0].toUpperCase() : '?'}
            </span>
          </div>
          {#if userName}
            <div class="text-center sm:text-left">
              <Card.Title class="text-2xl md:text-3xl font-bold">{userName}'s Profile</Card.Title>
              <Card.Description class="text-sm md:text-base mt-1">View {userName}'s posts and comments</Card.Description>
            </div>
          {/if}
        </div>
      </Card.Header>
    </Card.Root>

    <!-- Posts Section -->
    <Card.Root class="bg-opacity-90 mb-6 transition-all duration-200 hover:shadow-lg">
      <Card.Header class="flex flex-col sm:flex-row justify-between items-center gap-2 p-6">
        <div class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
          </svg>
          <Card.Title class="text-xl font-bold">Posts</Card.Title>
        </div>
        <span class="px-4 py-1.5 text-sm font-medium rounded-full bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-300 transition-colors">
          {threads.length} posts
        </span>
      </Card.Header>
      
      <!-- Loading State -->
      {#if loadingThreads}
        <Card.Content class="text-center py-16">
          <div class="inline-block h-10 w-10 animate-spin rounded-full border-4 border-solid border-current border-r-transparent motion-reduce:animate-[spin_1.5s_linear_infinite] opacity-75" role="status">
            <span class="!absolute !-m-px !h-px !w-px !overflow-hidden !whitespace-nowrap !border-0 !p-0 ![clip:rect(0,0,0,0)]">Loading posts...</span>
          </div>
        </Card.Content>
      {/if}

      <!-- No Posts State -->
      {#if !loadingThreads && threads.length === 0}
        <Card.Content class="text-center py-16">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="mx-auto w-16 h-16 text-neutral-400 mb-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
          <p class="text-neutral-500 dark:text-neutral-400 text-lg">No posts found</p>
        </Card.Content>
      {/if}

      <!-- Posts List -->
      {#if !loadingThreads && threads.length > 0}
        <div class="divide-y divide-neutral-200 dark:divide-neutral-700">
          {#each threads as thread}
            <div class="p-6 transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
              <div class="flex flex-col sm:flex-row justify-between items-start gap-4">
                <div class="flex-1 min-w-0">
                  <a href={`/thread/${thread.id}`} class="hover:underline block">
                    <h3 class="text-xl font-semibold line-clamp-2">{thread.title}</h3>
                  </a>
                  <p class="text-sm text-neutral-600 dark:text-neutral-400 line-clamp-2 mt-2">{thread.description}</p>
                  
                  {#if thread.tags && thread.tags.length > 0}
                    <div class="flex flex-wrap gap-2 mt-3">
                      {#each thread.resolvedTags as tag}
                        <span class="px-3 py-1 text-xs font-medium rounded-full bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-300 transition-colors">
                          {tag}
                        </span>
                      {/each}
                    </div>
                  {/if}
                  
                  <div class="text-sm text-neutral-500 dark:text-neutral-400 mt-3">
                    {formatDate(thread.createdAt)}
                  </div>
                </div>
                <Button variant="outline" size="sm" class="w-full sm:w-auto whitespace-nowrap hover:bg-rose-900 hover:text-white transition-colors duration-200">
                  <a href={`/thread/${thread.id}`} class="flex items-center justify-center gap-2">
                    View Post
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                </Button>
              </div>
            </div>
          {/each}
        </div>
      {/if}
    </Card.Root>

    <!-- Comments Section -->
    <Card.Root class="bg-opacity-90 transition-all duration-200 hover:shadow-lg">
      <Card.Header class="flex flex-col sm:flex-row justify-between items-center gap-2 p-6">
        <div class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
            <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 0 1 .865-.501 48.172 48.172 0 0 0 3.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0 0 12 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018Z" />
          </svg>
          <Card.Title class="text-xl font-bold">Comments</Card.Title>
        </div>
        <span class="px-4 py-1.5 text-sm font-medium rounded-full bg-neutral-100 text-neutral-800 dark:bg-neutral-950 dark:text-neutral-300 transition-colors">
          {comments.length} comments
        </span>
      </Card.Header>
      
      <!-- Loading State -->
      {#if loadingComments}
        <Card.Content class="text-center py-16">
          <div class="inline-block h-10 w-10 animate-spin rounded-full border-4 border-solid border-current border-r-transparent motion-reduce:animate-[spin_1.5s_linear_infinite] opacity-75" role="status">
            <span class="!absolute !-m-px !h-px !w-px !overflow-hidden !whitespace-nowrap !border-0 !p-0 ![clip:rect(0,0,0,0)]">Loading comments...</span>
          </div>
        </Card.Content>
      {/if}

      <!-- No Comments State -->
      {#if !loadingComments && comments.length === 0}
        <Card.Content class="text-center py-16">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="mx-auto w-16 h-16 text-neutral-400 mb-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.444 3 12c0 2.104.859 4.023 2.273 5.48.432.447.74 1.04.586 1.641a4.483 4.483 0 0 1-.923 1.785A5.969 5.969 0 0 0 6 21c1.282 0 2.47-.402 3.445-1.087.81.22 1.668.337 2.555.337Z" />
          </svg>
          <p class="text-neutral-500 dark:text-neutral-400 text-lg">No comments found</p>
        </Card.Content>
      {/if}

      <!-- Comments List -->
      {#if !loadingComments && comments.length > 0}
        <div class="divide-y divide-neutral-200 dark:divide-neutral-700">
          {#each comments as comment}
            <div class="p-6 transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-900/100">
              <div class="flex flex-col sm:flex-row justify-between items-start gap-4">
                <div class="flex-1 min-w-0">
                  <p class="text-sm md:text-base">{comment.content}</p>
                  <div class="text-sm text-neutral-500 dark:text-neutral-400 mt-3">
                    {formatDate(comment.createdAt)}
                  </div>
                </div>
                <Button variant="outline" size="sm" class="w-full sm:w-auto whitespace-nowrap hover:bg-rose-900 hover:text-white transition-colors duration-200">
                  <a href={`/thread/${comment.postId}`} class="flex items-center justify-center gap-2">
                    Go to Post
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 6H5.25A2.25 2.25 0 0 0 3 8.25v10.5A2.25 2.25 0 0 0 5.25 21h10.5A2.25 2.25 0 0 0 18 18.75V10.5m-10.5 6L21 3m0 0h-5.25M21 3v5.25" />
                    </svg>
                  </a>
                </Button>
              </div>
            </div>
          {/each}
        </div>
      {/if}
    </Card.Root>
  </div>
</div>