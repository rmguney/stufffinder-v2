<script>
    import { Separator } from "$lib/components/ui/separator";
    import * as Card from "$lib/components/ui/card/index.js";
</script>
    
<div class="flex justify-center bg-change dark:bg-dark shifting">
    <div class="lg:w-1/2 p-4 lg:py-8">
        <Card.Root class="bg-opacity-90 hover:bg-opacity-100">
            <Card.Header>
                <Card.Title class="pb-2 text-4xl">About</Card.Title>
            </Card.Header>
            <Separator class="my-2"/>

            <Card.Description class="p-4 text-lg">
                Stufffinder is a web application developed as part of the Bogazici University Software Engineering Master's Degree Program to demonstrate foundationary software engineering concepts, including requirements gathering process, modular architecture, frontend-backend integration, containerization, and testing practices and it is open sourced under MIT license for educational purposes.  It's main use case is helping users identify and explore objects using Wikidata, several other user defined properties, and community feedback the in the form of comments.           </Card.Description>
            <Separator class="my-2"/>

            <Card.Footer>
                <div class="flex h-5 space-x-4 text-lg">
                    <a href="https://github.com/rmguney/SWE573-2024F" class="hover:underline font-bold text-rose-900">Source</a>
                    <Separator orientation="vertical" class="py-4" />
                    <a href="https://github.com/rmguney/SWE573-2024F/wiki" class="hover:underline font-bold text-rose-900">Wiki</a>
                </div>
            </Card.Footer>   
        </Card.Root>
    </div>
</div>