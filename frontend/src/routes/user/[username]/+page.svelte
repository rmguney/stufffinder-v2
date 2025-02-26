<script>
  import * as Card from '$lib/components/ui/card/index.js';
  import { Button } from '$lib/components/ui/button';
  import Post from '$lib/components/post.svelte';
  import Comment from '$lib/components/comment.svelte';
  import { threadStore } from '../../../threadStore';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  
  // State variables
  let userId;
  let userName;
  let threads = [];
  let comments = [];
  let loadingThreads = true;
  let loadingComments = true;
  
  // Get username from URL params
  $: userName = $page.params.username;
  
  // Format date in the same style as the Angular implementation
  function formatDate(date) {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
  
  // Fetch user data, threads and comments
  onMount(async () => {
    try {
      loadingThreads = true;
      loadingComments = true;
      
      // First get the userId from the username
      const userResponse = await fetch(`http://localhost:8080/api/auth/username/${userName}`);
      if (!userResponse.ok) throw new Error('User not found');
      const userData = await userResponse.json();
      userId = userData.userId;
      
      // Fetch user's posts (threads)
      const postsResponse = await fetch(`http://localhost:8080/api/auth/${userId}/posts`);
      if (!postsResponse.ok) throw new Error('Failed to fetch posts');
      const postsData = await postsResponse.json();
      
      // Sort posts by most recent first, handling potential undefined dates
      threads = postsData.sort((a, b) => {
        const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return dateB - dateA;
      });
      
      // Update the thread store
      threadStore.set(threads);
      
      // Fetch user's comments
      const commentsResponse = await fetch(`http://localhost:8080/api/auth/${userId}/comments`);
      if (!commentsResponse.ok) throw new Error('Failed to fetch comments');
      const commentsData = await commentsResponse.json();
      
      // Sort comments by most recent first
      comments = commentsData.sort((a, b) => 
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      
    } catch (error) {
      console.error("Error fetching data:", error);
    } finally {
      loadingThreads = false;
      loadingComments = false;
    }
  });
</script>

<div class="flex justify-center p-6 lg:py-10 bg-change dark:bg-dark shifting">
  <div class="w-full lg:w-2/3">
    <!-- Profile Header -->
    <Card.Root class="bg-opacity-90 mb-6">
      <Card.Header class="flex items-center">
        <div class="flex items-center gap-4">
          <div class="bg-slate-100 dark:bg-slate-800 rounded-full p-3">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" />
            </svg>
          </div>
          {#if userName}
            <div>
              <Card.Title class="text-2xl">{userName}'s Profile</Card.Title>
              <Card.Description>View {userName}'s posts and comments</Card.Description>
            </div>
          {/if}
        </div>
      </Card.Header>
    </Card.Root>

    <!-- Posts Section -->
    <Card.Root class="bg-opacity-90 mb-6">
      <Card.Header class="flex justify-between items-center">
        <div class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
          </svg>
          <Card.Title>Posts</Card.Title>
        </div>
        <span class="px-3 py-1 text-xs font-medium rounded-full bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-300">
          {threads.length} posts
        </span>
      </Card.Header>
      
      <!-- Loading State -->
      {#if loadingThreads}
        <Card.Content class="text-center py-12">
          <div class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-current border-r-transparent motion-reduce:animate-[spin_1.5s_linear_infinite]" role="status">
            <span class="!absolute !-m-px !h-px !w-px !overflow-hidden !whitespace-nowrap !border-0 !p-0 ![clip:rect(0,0,0,0)]">Loading posts...</span>
          </div>
        </Card.Content>
      {/if}

      <!-- No Posts State -->
      {#if !loadingThreads && threads.length === 0}
        <Card.Content class="text-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="mx-auto w-12 h-12 text-slate-400 mb-3">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
          <p class="text-slate-500 dark:text-slate-400">No posts found</p>
        </Card.Content>
      {/if}

      <!-- Posts List -->
      {#if !loadingThreads && threads.length > 0}
        <div class="divide-y divide-slate-200 dark:divide-slate-700">
          {#each threads as thread}
            <div class="p-4">
              <div class="flex justify-between items-start gap-4">
                <div class="flex-1">
                  <a href={`/thread/${thread.id}`} class="hover:underline">
                    <h3 class="text-lg font-medium">{thread.title}</h3>
                  </a>
                  <p class="text-sm text-slate-600 dark:text-slate-400 line-clamp-2 mt-1">{thread.description}</p>
                  
                  {#if thread.tags && thread.tags.length > 0}
                    <div class="flex flex-wrap gap-1 mt-2">
                      {#each thread.tags as tag}
                        <span class="px-2 py-0.5 text-xs rounded-full bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300">
                          {tag}
                        </span>
                      {/each}
                    </div>
                  {/if}
                  
                  <div class="text-xs text-slate-500 dark:text-slate-400 mt-2">
                    {formatDate(thread.createdAt)}
                  </div>
                </div>
                <Button variant="outline" size="sm" class="whitespace-nowrap hover:bg-rose-900 hover:text-white">
                  <a href={`/thread/${thread.id}`} class="flex items-center gap-1">
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
    <Card.Root class="bg-opacity-90">
      <Card.Header class="flex justify-between items-center">
        <div class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 0 1 .865-.501 48.172 48.172 0 0 0 3.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0 0 12 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018Z" />
          </svg>
          <Card.Title>Comments</Card.Title>
        </div>
        <span class="px-3 py-1 text-xs font-medium rounded-full bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300">
          {comments.length} comments
        </span>
      </Card.Header>
      
      <!-- Loading State -->
      {#if loadingComments}
        <Card.Content class="text-center py-12">
          <div class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-current border-r-transparent motion-reduce:animate-[spin_1.5s_linear_infinite]" role="status">
            <span class="!absolute !-m-px !h-px !w-px !overflow-hidden !whitespace-nowrap !border-0 !p-0 ![clip:rect(0,0,0,0)]">Loading comments...</span>
          </div>
        </Card.Content>
      {/if}

      <!-- No Comments State -->
      {#if !loadingComments && comments.length === 0}
        <Card.Content class="text-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="mx-auto w-12 h-12 text-slate-400 mb-3">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.625 9.75a.375.375 0 0 0-.375.375v.875c0 .207.168.375.375.375h.875a.375.375 0 0 0 .375-.375v-.875a.375.375 0 0 0-.375-.375h-.875ZM12.75 9.75a.375.375 0 0 0-.375.375v.875c0 .207.168.375.375.375h.875a.375.375 0 0 0 .375-.375v-.875a.375.375 0 0 0-.375-.375h-.875ZM18.75 10.125c0 .207.168.375.375.375h.875a.375.375 0 0 0 .375-.375v-.875a.375.375 0 0 0-.375-.375h-.875a.375.375 0 0 0-.375.375v.875ZM7.5 12.375a.375.375 0 0 0-.375.375v.875c0 .207.168.375.375.375h.875a.375.375 0 0 0 .375-.375v-.875a.375.375 0 0 0-.375-.375H7.5ZM12.75 12.375a.375.375 0 0 0-.375.375v.875c0 .207.168.375.375.375h.875a.375.375 0 0 0 .375-.375v-.875a.375.375 0 0 0-.375-.375h-.875ZM12.75 15a.375.375 0 0 0-.375.375v.875c0 .207.168.375.375.375h.875a.375.375 0 0 0 .375-.375v-.875a.375.375 0 0 0-.375-.375h-.875ZM18.75 15.375c0 .207.168.375.375.375h.875a.375.375 0 0 0 .375-.375v-.875a.375.375 0 0 0-.375-.375h-.875a.375.375 0 0 0-.375.375v.875ZM7.5 15a.375.375 0 0 0-.375.375v.875c0 .207.168.375.375.375h.875a.375.375 0 0 0 .375-.375v-.875a.375.375 0 0 0-.375-.375H7.5Z" />
          </svg>
          <p class="text-slate-500 dark:text-slate-400">No comments found</p>
        </Card.Content>
      {/if}

      <!-- Comments List -->
      {#if !loadingComments && comments.length > 0}
        <div class="divide-y divide-slate-200 dark:divide-slate-700">
          {#each comments as comment}
            <div class="p-4">
              <div class="flex justify-between items-start gap-4">
                <div class="flex-1">
                  <p class="text-sm">{comment.content}</p>
                  <div class="text-xs text-slate-500 dark:text-slate-400 mt-2">
                    {formatDate(comment.createdAt)}
                  </div>
                </div>
                <Button variant="outline" size="sm" class="whitespace-nowrap hover:bg-rose-900 hover:text-white">
                  <a href={`/thread/${comment.postId}`} class="flex items-center gap-1">
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